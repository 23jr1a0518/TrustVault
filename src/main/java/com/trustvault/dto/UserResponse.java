package com.trustvault.dto;

import com.trustvault.model.AccountStatus;
import com.trustvault.model.Role;
import com.trustvault.model.User;

import java.time.Instant;

public class UserResponse {
    private String id;
    private String email;
    private Role role;
    private AccountStatus status;
    private Instant createdAt;

    public UserResponse() {}

    public UserResponse(String id, String email, Role role, AccountStatus status, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static UserResponse from(User user) {
        if (user == null) return null;
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt()
        );
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
