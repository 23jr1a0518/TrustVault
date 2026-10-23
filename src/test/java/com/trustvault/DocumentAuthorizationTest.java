package com.trustvault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustvault.dto.CreateGrantRequest;
import com.trustvault.dto.DocumentResponse;
import com.trustvault.dto.DocumentUpdateRequest;
import com.trustvault.model.AccountStatus;
import com.trustvault.model.PermissionLevel;
import com.trustvault.model.Role;
import com.trustvault.model.User;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DocumentAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private User viewerC;
    private CustomUserDetails userDetailsA;
    private CustomUserDetails userDetailsB;
    private CustomUserDetails userDetailsC;

    private DocumentResponse docA;

    @BeforeEach
    void setUp() {
        userA = new User(
            "u-a-" + UUID.randomUUID(),
            "alice-" + UUID.randomUUID() + "@trustvault.local",
            passwordEncoder.encode("Pass123!"),
            Role.OWNER,
            AccountStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        );
        userRepository.save(userA);
        userDetailsA = new CustomUserDetails(userA);

        userB = new User(
            "u-b-" + UUID.randomUUID(),
            "bob-" + UUID.randomUUID() + "@trustvault.local",
            passwordEncoder.encode("Pass123!"),
            Role.OWNER,
            AccountStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        );
        userRepository.save(userB);
        userDetailsB = new CustomUserDetails(userB);

        viewerC = new User(
            "u-c-" + UUID.randomUUID(),
            "charlie-" + UUID.randomUUID() + "@trustvault.local",
            passwordEncoder.encode("Pass123!"),
            Role.VIEWER,
            AccountStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        );
        userRepository.save(viewerC);
        userDetailsC = new CustomUserDetails(viewerC);

        // Upload a document as User A
        byte[] pdfContent = "%PDF-1.5 test content alice".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "alice_confidential.pdf", "application/pdf", pdfContent
        );
        docA = documentService.uploadDocument(file, "FINANCE", "Confidential financial report", userA, "req-setup");
    }

    @Test
    @DisplayName("User B cannot view metadata of User A's document without a grant (returns 403)")
    void testUserBCannotReadUserADocument() throws Exception {
        mockMvc.perform(get("/api/documents/" + docA.getId())
                .with(user(userDetailsB)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("User B cannot download User A's document without a grant (returns 403)")
    void testUserBCannotDownloadUserADocument() throws Exception {
        mockMvc.perform(get("/api/documents/" + docA.getId() + "/download")
                .with(user(userDetailsB)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("User B cannot edit metadata of User A's document (returns 403)")
    void testUserBCannotEditUserADocument() throws Exception {
        DocumentUpdateRequest updateReq = new DocumentUpdateRequest("tampered.pdf", "LEGAL", "Hacked");

        mockMvc.perform(patch("/api/documents/" + docA.getId())
                .with(user(userDetailsB))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("User B cannot delete User A's document (returns 403)")
    void testUserBCannotDeleteUserADocument() throws Exception {
        mockMvc.perform(delete("/api/documents/" + docA.getId())
                .with(user(userDetailsB))
                .with(csrf()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("User A grants READ access to User B: User B can read/download, but cannot edit or grant")
    void testGrantAndRevocationLifecycle() throws Exception {
        // 1. User A grants READ to User B
        CreateGrantRequest grantReq = new CreateGrantRequest(userB.getEmail(), PermissionLevel.READ);

        MvcResult grantResult = mockMvc.perform(post("/api/documents/" + docA.getId() + "/grants")
                .with(user(userDetailsA))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(grantReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recipientEmail").value(userB.getEmail()))
            .andExpect(jsonPath("$.permissionLevel").value("READ"))
            .andReturn();

        String grantId = objectMapper.readTree(grantResult.getResponse().getContentAsString()).get("id").asText();

        // 2. User B can now read and download
        mockMvc.perform(get("/api/documents/" + docA.getId())
                .with(user(userDetailsB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(docA.getId()))
            .andExpect(jsonPath("$.permissionLevel").value("READ"));

        mockMvc.perform(get("/api/documents/" + docA.getId() + "/download")
                .with(user(userDetailsB)))
            .andExpect(status().isOk());

        // 3. User B (with READ only) cannot edit metadata
        DocumentUpdateRequest updateReq = new DocumentUpdateRequest("mod.pdf", "FINANCE", "Attempted edit");
        mockMvc.perform(patch("/api/documents/" + docA.getId())
                .with(user(userDetailsB))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isForbidden());

        // 4. User B cannot grant permissions to Charlie (only owner can)
        CreateGrantRequest bGrantReq = new CreateGrantRequest(viewerC.getEmail(), PermissionLevel.READ);
        mockMvc.perform(post("/api/documents/" + docA.getId() + "/grants")
                .with(user(userDetailsB))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bGrantReq)))
            .andExpect(status().isForbidden());

        // 5. User A revokes grant -> User B's immediate next request is blocked with 403
        mockMvc.perform(delete("/api/documents/" + docA.getId() + "/grants/" + grantId)
                .with(user(userDetailsA))
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents/" + docA.getId())
                .with(user(userDetailsB)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Search isolation: Documents do not leak across users, and SQL injection payloads are neutralized")
    void testSearchIsolationAndSqlInjectionResistance() throws Exception {
        // User A searches: finds Alice's document
        mockMvc.perform(get("/api/documents?name=alice")
                .with(user(userDetailsA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].displayFilename").value("alice_confidential.pdf"));

        // User B searches: cannot find Alice's document
        mockMvc.perform(get("/api/documents?name=alice")
                .with(user(userDetailsB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        // SQL injection attempt in search filter does not bypass isolation
        mockMvc.perform(get("/api/documents?name=' OR '1'='1' --")
                .with(user(userDetailsB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
