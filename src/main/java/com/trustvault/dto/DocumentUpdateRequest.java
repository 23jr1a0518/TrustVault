package com.trustvault.dto;

import jakarta.validation.constraints.Size;

public class DocumentUpdateRequest {
    @Size(min = 1, max = 255, message = "Display filename must be between 1 and 255 characters")
    private String displayFilename;

    @Size(max = 64, message = "Category must be at most 64 characters")
    private String category;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    public DocumentUpdateRequest() {}

    public DocumentUpdateRequest(String displayFilename, String category, String description) {
        this.displayFilename = displayFilename;
        this.category = category;
        this.description = description;
    }

    public String getDisplayFilename() { return displayFilename; }
    public void setDisplayFilename(String displayFilename) { this.displayFilename = displayFilename; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
