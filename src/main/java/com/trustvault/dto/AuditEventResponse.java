package com.trustvault.dto;

import com.trustvault.model.AuditEvent;
import com.trustvault.model.AuditOutcome;

import java.time.Instant;

public class AuditEventResponse {
    private String id;
    private String actorId;
    private String actorEmail;
    private String action;
    private String documentId;
    private String documentName;
    private String targetUserId;
    private String targetUserEmail;
    private AuditOutcome outcome;
    private Instant serverTimestamp;
    private String requestId;
    private String sanitizedDetails;
    private String previousHash;
    private String currentHash;

    public AuditEventResponse() {}

    public AuditEventResponse(String id, String actorId, String actorEmail, String action,
                              String documentId, String documentName, String targetUserId,
                              String targetUserEmail, AuditOutcome outcome, Instant serverTimestamp,
                              String requestId, String sanitizedDetails, String previousHash, String currentHash) {
        this.id = id;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.documentId = documentId;
        this.documentName = documentName;
        this.targetUserId = targetUserId;
        this.targetUserEmail = targetUserEmail;
        this.outcome = outcome;
        this.serverTimestamp = serverTimestamp;
        this.requestId = requestId;
        this.sanitizedDetails = sanitizedDetails;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
    }

    public static AuditEventResponse from(AuditEvent event, String actorEmail, String documentName, String targetUserEmail) {
        return new AuditEventResponse(
            event.getId(),
            event.getActorId(),
            actorEmail,
            event.getAction(),
            event.getDocumentId(),
            documentName,
            event.getTargetUserId(),
            targetUserEmail,
            event.getOutcome(),
            event.getServerTimestamp(),
            event.getRequestId(),
            event.getSanitizedDetails(),
            event.getPreviousHash(),
            event.getCurrentHash()
        );
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public String getTargetUserEmail() { return targetUserEmail; }
    public void setTargetUserEmail(String targetUserEmail) { this.targetUserEmail = targetUserEmail; }

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
