package com.trustvault.dto;

import com.trustvault.model.Document;
import com.trustvault.model.PermissionLevel;

import java.time.Instant;

public class DocumentResponse {
    private String id;
    private String ownerId;
    private String ownerEmail;
    private String displayFilename;
    private String mimeType;
    private long fileSize;
    private String category;
    private String description;
    private String checksumSha256;
    private boolean isOwner;
    private PermissionLevel permissionLevel;
    private Instant createdAt;
    private Instant updatedAt;

    public DocumentResponse() {}

    public DocumentResponse(String id, String ownerId, String ownerEmail, String displayFilename,
                            String mimeType, long fileSize, String category, String description,
                            String checksumSha256, boolean isOwner, PermissionLevel permissionLevel,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerEmail = ownerEmail;
        this.displayFilename = displayFilename;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.category = category;
        this.description = description;
        this.checksumSha256 = checksumSha256;
        this.isOwner = isOwner;
        this.permissionLevel = permissionLevel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DocumentResponse from(Document doc, String ownerEmail, boolean isOwner, PermissionLevel permissionLevel) {
        return new DocumentResponse(
            doc.getId(),
            doc.getOwnerId(),
            ownerEmail,
            doc.getDisplayFilename(),
            doc.getMimeType(),
            doc.getFileSize(),
            doc.getCategory(),
            doc.getDescription(),
            doc.getChecksumSha256(),
            isOwner,
            permissionLevel,
            doc.getCreatedAt(),
            doc.getUpdatedAt()
        );
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getDisplayFilename() { return displayFilename; }
    public void setDisplayFilename(String displayFilename) { this.displayFilename = displayFilename; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public boolean isOwner() { return isOwner; }
    public void setOwner(boolean owner) { isOwner = owner; }

    public PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
