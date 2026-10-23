package com.trustvault.dto;

import com.trustvault.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class UserStatusUpdateRequest {
    @NotNull(message = "Account status is required")
    private AccountStatus status;

    public UserStatusUpdateRequest() {}

    public UserStatusUpdateRequest(AccountStatus status) {
        this.status = status;
    }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
