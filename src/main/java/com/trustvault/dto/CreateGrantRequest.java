package com.trustvault.dto;

import com.trustvault.model.PermissionLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateGrantRequest {
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid recipient email format")
    private String recipientEmail;

    @NotNull(message = "Permission level is required (READ or EDIT)")
    private PermissionLevel permissionLevel;

    private Integer durationHours;

    public CreateGrantRequest() {}

    public CreateGrantRequest(String recipientEmail, PermissionLevel permissionLevel) {
        this(recipientEmail, permissionLevel, null);
    }

    public CreateGrantRequest(String recipientEmail, PermissionLevel permissionLevel, Integer durationHours) {
        this.recipientEmail = recipientEmail;
        this.permissionLevel = permissionLevel;
        this.durationHours = durationHours;
    }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
}
