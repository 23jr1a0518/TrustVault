package com.trustvault.model;

import java.time.Instant;

public class DocumentGrant {
    private String id;
    private String documentId;
    private String recipientId;
    private PermissionLevel permissionLevel;
    private String grantedBy;
    private Instant grantTimestamp;
    private Instant expiresAt;
    private Instant revocationTimestamp;
    private String revokedBy;

    public DocumentGrant() {}

    public DocumentGrant(String id, String documentId, String recipientId,
                         PermissionLevel permissionLevel, String grantedBy,
                         Instant grantTimestamp, Instant revocationTimestamp, String revokedBy) {
        this(id, documentId, recipientId, permissionLevel, grantedBy, grantTimestamp, null, revocationTimestamp, revokedBy);
    }

    public DocumentGrant(String id, String documentId, String recipientId,
                         PermissionLevel permissionLevel, String grantedBy,
                         Instant grantTimestamp, Instant expiresAt,
                         Instant revocationTimestamp, String revokedBy) {
        this.id = id;
        this.documentId = documentId;
        this.recipientId = recipientId;
        this.permissionLevel = permissionLevel;
        this.grantedBy = grantedBy;
        this.grantTimestamp = grantTimestamp;
        this.expiresAt = expiresAt;
        this.revocationTimestamp = revocationTimestamp;
        this.revokedBy = revokedBy;
    }

    public boolean isActive() {
        if (revocationTimestamp != null) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }

    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }

    public Instant getGrantTimestamp() { return grantTimestamp; }
    public void setGrantTimestamp(Instant grantTimestamp) { this.grantTimestamp = grantTimestamp; }

    public Instant getRevocationTimestamp() { return revocationTimestamp; }
    public void setRevocationTimestamp(Instant revocationTimestamp) { this.revocationTimestamp = revocationTimestamp; }

    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
}
