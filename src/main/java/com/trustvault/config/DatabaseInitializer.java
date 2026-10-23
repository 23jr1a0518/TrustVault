package com.trustvault.config;

import com.trustvault.audit.AuditService;
import com.trustvault.model.AccountStatus;
import com.trustvault.model.AuditOutcome;
import com.trustvault.model.Role;
import com.trustvault.model.User;
import com.trustvault.repository.AuditRepository;
import com.trustvault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Value("${trustvault.admin.email:admin@trustvault.local}")
    private String adminEmail;

    @Value("${trustvault.admin.password:AdminPass123!}")
    private String adminPassword;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate,
                               UserRepository userRepository,
                               AuditRepository auditRepository,
                               AuditService auditService,
                               PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Checking database tables and schema initialization...");
        try {
            ensureSchema();
            seedDefaultUsersIfEmpty();
        } catch (Exception e) {
            log.error("Database initialization check encountered error: {}", e.getMessage(), e);
        }
    }

    private void ensureSchema() {
        try {
            // Check if 'users' table exists
            jdbcTemplate.execute("SELECT 1 FROM users LIMIT 1");
            log.info("Database schema already present.");
        } catch (Exception e) {
            log.info("Schema not found or uninitialized. Applying database/schema.sql...");
            try {
                ClassPathResource resource = new ClassPathResource("schema.sql");
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        for (String stmt : sql.split(";")) {
                            if (!stmt.trim().isBlank()) {
                                jdbcTemplate.execute(stmt.trim());
                            }
                        }
                        log.info("Successfully executed schema DDL.");
                    }
                }
            } catch (Exception ex) {
                log.warn("Auto-schema execution failed or skipped: {}", ex.getMessage());
            }
        }
    }

    private void seedDefaultUsersIfEmpty() {
        if (userRepository.count() == 0) {
            log.info("Seeding initial administrator user: {}", adminEmail);

            String adminId = "u-admin-00000000-0000-0000-000000000001";
            User admin = new User(
                adminId,
                adminEmail,
                passwordEncoder.encode(adminPassword),
                Role.ADMIN,
                AccountStatus.ACTIVE,
                Instant.now(),
                Instant.now()
            );
            userRepository.save(admin);

            // Also seed demo owner and viewer for out-of-the-box local testing
            User owner = new User(
                "u-owner-00000000-0000-0000-000000000002",
                "owner@trustvault.local",
                passwordEncoder.encode("OwnerPass123!"),
                Role.OWNER,
                AccountStatus.ACTIVE,
                Instant.now(),
                Instant.now()
            );
            userRepository.save(owner);

            User viewer = new User(
                "u-viewr-00000000-0000-0000-000000000003",
                "viewer@trustvault.local",
                passwordEncoder.encode("ViewerPass123!"),
                Role.VIEWER,
                AccountStatus.ACTIVE,
                Instant.now(),
                Instant.now()
            );
            userRepository.save(viewer);

            User disabled = new User(
                "u-disab-00000000-0000-0000-000000000004",
                "disabled@trustvault.local",
                passwordEncoder.encode("DisabledPass123!"),
                Role.VIEWER,
                AccountStatus.DISABLED,
                Instant.now(),
                Instant.now()
            );
            userRepository.save(disabled);

            log.info("Default users seeded: admin, owner, viewer, disabled");

            // Seed genesis audit log
            if (auditRepository.count() == 0) {
                auditService.logEvent(
                    adminId,
                    "SYSTEM_GENESIS",
                    null,
                    null,
                    AuditOutcome.SUCCESS,
                    "req-genesis-" + UUID.randomUUID(),
                    "TrustVault Audit Log Genesis Node Initialized"
                );
            }
        }
    }
}
