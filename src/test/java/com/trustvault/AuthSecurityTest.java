package com.trustvault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustvault.dto.LoginRequest;
import com.trustvault.model.AccountStatus;
import com.trustvault.model.Role;
import com.trustvault.model.User;
import com.trustvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        String testEmail = "testuser-" + UUID.randomUUID() + "@trustvault.local";
        testUser = new User(
            "u-test-" + UUID.randomUUID(),
            testEmail,
            passwordEncoder.encode("SecretPass123!"),
            Role.OWNER,
            AccountStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        );
        userRepository.save(testUser);

        String disabledEmail = "disabled-" + UUID.randomUUID() + "@trustvault.local";
        disabledUser = new User(
            "u-dis-" + UUID.randomUUID(),
            disabledEmail,
            passwordEncoder.encode("SecretPass123!"),
            Role.VIEWER,
            AccountStatus.DISABLED,
            Instant.now(),
            Instant.now()
        );
        userRepository.save(disabledUser);
    }

    @Test
    @DisplayName("Anonymous request to protected endpoint /api/documents is rejected with 401 Unauthorized")
    void testAnonymousAccessDenied() throws Exception {
        mockMvc.perform(get("/api/documents"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Valid credentials authenticate and rotate session ID for session fixation protection")
    void testLoginRotatesSessionId() throws Exception {
        MockHttpSession preSession = new MockHttpSession();
        String initialSessionId = preSession.getId();

        LoginRequest loginReq = new LoginRequest(testUser.getEmail(), "SecretPass123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .session(preSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(testUser.getEmail()))
            .andReturn();

        MockHttpSession postSession = (MockHttpSession) result.getRequest().getSession(false);
        assertNotEquals(initialSessionId, postSession.getId(), "Session ID must change on login to prevent fixation");
    }

    @Test
    @DisplayName("Disabled account cannot authenticate and is rejected with 401")
    void testDisabledAccountRejected() throws Exception {
        LoginRequest loginReq = new LoginRequest(disabledUser.getEmail(), "SecretPass123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CSRF protection blocks state-changing POST requests without CSRF token")
    @WithMockUser(username = "admin@trustvault.local", roles = {"ADMIN"})
    void testCsrfProtectionEnforced() throws Exception {
        mockMvc.perform(post("/api/admin/users/test/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-admin user cannot access admin audit endpoints (returns 403 Forbidden)")
    @WithMockUser(username = "owner@trustvault.local", roles = {"OWNER"})
    void testNonAdminCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/audit"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
