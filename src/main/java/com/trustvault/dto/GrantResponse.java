package com.trustvault.dto;

import com.trustvault.model.DocumentGrant;
import com.trustvault.model.PermissionLevel;

import java.time.Instant;

public class GrantResponse {
    private String id;
    private String documentId;
    private String recipientId;
    private String recipientEmail;
    private PermissionLevel permissionLevel;
    private String grantedBy;
    private String grantedByEmail;
    private Instant grantTimestamp;
    private Instant revocationTimestamp;
    private String revokedBy;
    private String revokedByEmail;
    private boolean isActive;

    public GrantResponse() {}

    public GrantResponse(String id, String documentId, String recipientId, String recipientEmail,
                         PermissionLevel permissionLevel, String grantedBy, String grantedByEmail,
                         Instant grantTimestamp, Instant revocationTimestamp, String revokedBy,
                         String revokedByEmail, boolean isActive) {
        this.id = id;
        this.documentId = documentId;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
        this.permissionLevel = permissionLevel;
        this.grantedBy = grantedBy;
        this.grantedByEmail = grantedByEmail;
        this.grantTimestamp = grantTimestamp;
        this.revocationTimestamp = revocationTimestamp;
        this.revokedBy = revokedBy;
        this.revokedByEmail = revokedByEmail;
        this.isActive = isActive;
    }

    public static GrantResponse from(DocumentGrant grant, String recipientEmail, String grantedByEmail, String revokedByEmail) {
        return new GrantResponse(
            grant.getId(),
            grant.getDocumentId(),
            grant.getRecipientId(),
            recipientEmail,
            grant.getPermissionLevel(),
            grant.getGrantedBy(),
            grantedByEmail,
            grant.getGrantTimestamp(),
            grant.getRevocationTimestamp(),
            grant.getRevokedBy(),
            revokedByEmail,
            grant.isActive()
        );
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }

    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }

    public String getGrantedByEmail() { return grantedByEmail; }
    public void setGrantedByEmail(String grantedByEmail) { this.grantedByEmail = grantedByEmail; }

    public Instant getGrantTimestamp() { return grantTimestamp; }
    public void setGrantTimestamp(Instant grantTimestamp) { this.grantTimestamp = grantTimestamp; }

    public Instant getRevocationTimestamp() { return revocationTimestamp; }
    public void setRevocationTimestamp(Instant revocationTimestamp) { this.revocationTimestamp = revocationTimestamp; }

    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }

    public String getRevokedByEmail() { return revokedByEmail; }
    public void setRevokedByEmail(String revokedByEmail) { this.revokedByEmail = revokedByEmail; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
