package com.trustvault.model;

import java.time.Instant;

public class AuditEvent {
    private String id;
    private String actorId;
    private String action;
    private String documentId;
    private String targetUserId;
    private AuditOutcome outcome;
    private Instant serverTimestamp;
    private String requestId;
    private String sanitizedDetails;
    private String previousHash;
    private String currentHash;

    public AuditEvent() {}

    public AuditEvent(String id, String actorId, String action, String documentId,
                      String targetUserId, AuditOutcome outcome, Instant serverTimestamp,
                      String requestId, String sanitizedDetails, String previousHash, String currentHash) {
        this.id = id;
        this.actorId = actorId;
        this.action = action;
        this.documentId = documentId;
        this.targetUserId = targetUserId;
        this.outcome = outcome;
        this.serverTimestamp = serverTimestamp;
        this.requestId = requestId;
        this.sanitizedDetails = sanitizedDetails;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public AuditOutcome getOutcome() { return outcome; }
    public void setOutcome(AuditOutcome outcome) { this.outcome = outcome; }

    public Instant getServerTimestamp() { return serverTimestamp; }
    public void setServerTimestamp(Instant serverTimestamp) { this.serverTimestamp = serverTimestamp; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSanitizedDetails() { return sanitizedDetails; }
    public void setSanitizedDetails(String sanitizedDetails) { this.sanitizedDetails = sanitizedDetails; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }
}
