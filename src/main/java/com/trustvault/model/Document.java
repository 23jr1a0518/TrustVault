package com.trustvault.model;

import java.time.Instant;

public class Document {
    private String id;
    private String ownerId;
    private String displayFilename;
    private String storageObjectKey;
    private String mimeType;
    private long fileSize;
    private String category;
    private String description;
    private String checksumSha256;
    private boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;

    public Document() {}

    public Document(String id, String ownerId, String displayFilename, String storageObjectKey,
                    String mimeType, long fileSize, String category, String description,
                    String checksumSha256, boolean isDeleted, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.displayFilename = displayFilename;
        this.storageObjectKey = storageObjectKey;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.category = category;
        this.description = description;
        this.checksumSha256 = checksumSha256;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getDisplayFilename() { return displayFilename; }
    public void setDisplayFilename(String displayFilename) { this.displayFilename = displayFilename; }

    public String getStorageObjectKey() { return storageObjectKey; }
    public void setStorageObjectKey(String storageObjectKey) { this.storageObjectKey = storageObjectKey; }

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

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
