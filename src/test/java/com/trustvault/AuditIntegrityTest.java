package com.trustvault;

import com.trustvault.audit.AuditService;
import com.trustvault.dto.AuditIntegrityReport;
import com.trustvault.model.AccountStatus;
import com.trustvault.model.AuditEvent;
import com.trustvault.model.AuditOutcome;
import com.trustvault.model.Role;
import com.trustvault.model.User;
import com.trustvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AuditIntegrityTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User auditActor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_events");
        auditActor = new User(
            "u-audit-" + UUID.randomUUID(),
            "auditor-" + UUID.randomUUID() + "@trustvault.local",
            passwordEncoder.encode("Pass123!"),
            Role.ADMIN,
            AccountStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        );
        userRepository.save(auditActor);
    }

    @Test
    @DisplayName("Cryptographic hash chain maintains integrity across sequential audit events")
    void testAuditChainIntegritySuccess() {
        // Log a sequence of diverse events with valid actor
        for (int i = 1; i <= 5; i++) {
            auditService.logEvent(
                auditActor.getId(),
                "ACTION_" + i,
                null,
                null,
                AuditOutcome.SUCCESS,
                "req-seq-" + i,
                "Deterministic verification event #" + i
            );
        }

        AuditIntegrityReport report = auditService.verifyChainIntegrity();
        assertTrue(report.isValid(), "Audit chain must verify as valid when unmodified");
        assertEquals("CHAIN_INTACT", report.getStatus());
        assertTrue(report.getTotalEventsVerified() >= 5);
        assertNotNull(report.getLatestHash());
    }

    @Test
    @DisplayName("Audit tampering detection: Direct database modification is flagged immediately with broken event ID")
    void testAuditTamperingDetection() {
        // Record an event to tamper with
        String uniqueMarker = "tamper-target-" + UUID.randomUUID();
        AuditEvent event = auditService.logEvent(
            auditActor.getId(),
            "ADMIN_CONFIG_CHANGE",
            null,
            null,
            AuditOutcome.SUCCESS,
            "req-tamper-target",
            uniqueMarker
        );

        // Record a subsequent event so the chain continues past it
        auditService.logEvent(
            auditActor.getId(),
            "DOCUMENT_READ",
            null,
            null,
            AuditOutcome.SUCCESS,
            "req-followup",
            "Subsequent event"
        );

        // Tamper directly with the recorded details in the database behind the application's back
        jdbcTemplate.update(
            "UPDATE audit_events SET sanitized_details = 'MALICIOUS_FORGERY_CONTENT' WHERE id = ?",
            event.getId()
        );

        // Run integrity verification engine
        AuditIntegrityReport report = auditService.verifyChainIntegrity();

        assertFalse(report.isValid(), "Tampered audit chain must fail cryptographic verification");
        assertEquals("TAMPERING_DETECTED", report.getStatus());
        assertEquals(event.getId(), report.getBrokenAtEventId(), "Verification must pinpoint the exact tampered record");
    }

    @Test
    @DisplayName("Denied operations record DENIED audit outcomes without leaking secrets")
    void testDeniedAuditsRecorded() {
        String reqId = "req-denied-" + UUID.randomUUID();
        // Unauthenticated or unknown actor ID can be null in audit logs
        AuditEvent event = auditService.logEvent(
            null,
            "DOCUMENT_DOWNLOAD",
            null,
            null,
            AuditOutcome.DENIED,
            reqId,
            "Unauthorized download attempt intercepted"
        );

        assertNotNull(event);
        assertEquals(AuditOutcome.DENIED, event.getOutcome());
        assertFalse(event.getSanitizedDetails().contains("password"));
        assertFalse(event.getSanitizedDetails().contains("secret"));
    }
}
