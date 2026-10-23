package com.trustvault.dto;

import java.time.Instant;

public class AuditIntegrityReport {
    private boolean valid;
    private long totalEventsVerified;
    private String status;
    private String brokenAtEventId;
    private String calculatedHash;
    private String recordedHash;
    private String latestHash;
    private Instant verificationTimestamp;

    public AuditIntegrityReport() {}

    public AuditIntegrityReport(boolean valid, long totalEventsVerified, String status,
                                String brokenAtEventId, String calculatedHash, String recordedHash,
                                String latestHash, Instant verificationTimestamp) {
        this.valid = valid;
        this.totalEventsVerified = totalEventsVerified;
        this.status = status;
        this.brokenAtEventId = brokenAtEventId;
        this.calculatedHash = calculatedHash;
        this.recordedHash = recordedHash;
        this.latestHash = latestHash;
        this.verificationTimestamp = verificationTimestamp;
    }

    public static AuditIntegrityReport success(long totalEvents, String latestHash) {
        return new AuditIntegrityReport(
            true,
            totalEvents,
            "CHAIN_INTACT",
            null,
            null,
            null,
            latestHash,
            Instant.now()
        );
    }

    public static AuditIntegrityReport tampered(long totalEvents, String brokenId, String calc, String rec) {
        return new AuditIntegrityReport(
            false,
            totalEvents,
            "TAMPERING_DETECTED",
            brokenId,
            calc,
            rec,
            null,
            Instant.now()
        );
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public long getTotalEventsVerified() { return totalEventsVerified; }
    public void setTotalEventsVerified(long totalEventsVerified) { this.totalEventsVerified = totalEventsVerified; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBrokenAtEventId() { return brokenAtEventId; }
    public void setBrokenAtEventId(String brokenAtEventId) { this.brokenAtEventId = brokenAtEventId; }

    public String getCalculatedHash() { return calculatedHash; }
    public void setCalculatedHash(String calculatedHash) { this.calculatedHash = calculatedHash; }

    public String getRecordedHash() { return recordedHash; }
    public void setRecordedHash(String recordedHash) { this.recordedHash = recordedHash; }

    public String getLatestHash() { return latestHash; }
    public void setLatestHash(String latestHash) { this.latestHash = latestHash; }

    public Instant getVerificationTimestamp() { return verificationTimestamp; }
    public void setVerificationTimestamp(Instant verificationTimestamp) { this.verificationTimestamp = verificationTimestamp; }
}
