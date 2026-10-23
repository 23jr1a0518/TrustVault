package com.trustvault.audit;

import com.trustvault.dto.AuditEventResponse;
import com.trustvault.dto.AuditIntegrityReport;
import com.trustvault.model.AuditEvent;
import com.trustvault.model.AuditOutcome;
import com.trustvault.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditRepository auditRepository;
    private final ReentrantLock chainLock = new ReentrantLock();

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Record an immutable, tamper-evident audit event into the SHA-256 hash chain.
     */
    public AuditEvent logEvent(String actorId, String action, String documentId,
                               String targetUserId, AuditOutcome outcome,
                               String requestId, String sanitizedDetails) {
        chainLock.lock();
        try {
            String eventId = "ae-" + UUID.randomUUID().toString();
            Instant now = Instant.now();

            String previousHash = auditRepository.findLatestEvent()
                .map(AuditEvent::getCurrentHash)
                .orElse(GENESIS_HASH);

            String currentHash = calculateHash(
                previousHash, eventId, actorId, action,
                documentId, targetUserId, outcome, now, requestId, sanitizedDetails
            );

            AuditEvent event = new AuditEvent(
                eventId,
                actorId,
                action,
                documentId,
                targetUserId,
                outcome,
                now,
                requestId != null ? requestId : "req-system",
                sanitizedDetails,
                previousHash,
                currentHash
            );

            auditRepository.save(event);
            log.info("AUDIT [{}] actor={}, action={}, doc={}, outcome={}, hash={}",
                event.getRequestId(), actorId, action, documentId, outcome, currentHash);
            return event;
        } catch (Exception e) {
            log.error("CRITICAL: Failed to write audit event for action {}: {}", action, e.getMessage(), e);
            // Fail-secure: If audit write fails, propagate exception so sensitive state is rolled back
            throw new IllegalStateException("Security audit logging failure: " + e.getMessage(), e);
        } finally {
            chainLock.unlock();
        }
    }

    /**
     * Deterministically calculates SHA-256 hash of the audit event linked to previous hash.
     */
    public String calculateHash(String previousHash, String eventId, String actorId, String action,
                                String documentId, String targetUserId, AuditOutcome outcome,
                                Instant timestamp, String requestId, String sanitizedDetails) {
        String canonical = String.format(
            "%s|%s|%s|%s|%s|%s|%s|%d|%s|%s",
            previousHash != null ? previousHash : GENESIS_HASH,
            eventId,
            actorId != null ? actorId : "",
            action,
            documentId != null ? documentId : "",
            targetUserId != null ? targetUserId : "",
            outcome.name(),
            timestamp != null ? timestamp.toEpochMilli() : 0L,
            requestId != null ? requestId : "",
            sanitizedDetails != null ? sanitizedDetails : ""
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    /**
     * Verifies the cryptographic integrity of the entire audit chain.
     */
    public AuditIntegrityReport verifyChainIntegrity() {
        chainLock.lock();
        try {
            List<AuditEvent> events = auditRepository.findAllChronological();
            if (events.isEmpty()) {
                return AuditIntegrityReport.success(0, GENESIS_HASH);
            }

            String expectedPreviousHash = GENESIS_HASH;

            for (int i = 0; i < events.size(); i++) {
                AuditEvent current = events.get(i);

                // 1. Verify link to previous event
                if (!current.getPreviousHash().equalsIgnoreCase(expectedPreviousHash)) {
                    log.warn("Integrity breach at event {}: previousHash mismatch. Expected {}, found {}",
                        current.getId(), expectedPreviousHash, current.getPreviousHash());
                    return AuditIntegrityReport.tampered(
                        events.size(),
                        current.getId(),
                        expectedPreviousHash,
                        current.getPreviousHash()
                    );
                }

                // 2. Recalculate hash of current event
                String recalculated = calculateHash(
                    current.getPreviousHash(),
                    current.getId(),
                    current.getActorId(),
                    current.getAction(),
                    current.getDocumentId(),
                    current.getTargetUserId(),
                    current.getOutcome(),
                    current.getServerTimestamp(),
                    current.getRequestId(),
                    current.getSanitizedDetails()
                );

                if (!recalculated.equalsIgnoreCase(current.getCurrentHash())) {
                    log.warn("Integrity breach at event {}: currentHash mismatch. Computed {}, recorded {}",
                        current.getId(), recalculated, current.getCurrentHash());
                    return AuditIntegrityReport.tampered(
                        events.size(),
                        current.getId(),
                        recalculated,
                        current.getCurrentHash()
                    );
                }

                expectedPreviousHash = current.getCurrentHash();
            }

            return AuditIntegrityReport.success(events.size(), expectedPreviousHash);
        } finally {
            chainLock.unlock();
        }
    }

    public List<AuditEventResponse> getPaginatedAudit(int limit, int offset) {
        return auditRepository.findAllPaginated(limit, offset);
    }

    public List<AuditEventResponse> getDocumentAudit(String documentId, int limit, int offset) {
        return auditRepository.findByDocumentId(documentId, limit, offset);
    }
}
