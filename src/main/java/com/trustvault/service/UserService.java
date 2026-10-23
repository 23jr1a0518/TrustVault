package com.trustvault.service;

import com.trustvault.audit.AuditService;
import com.trustvault.dto.UserResponse;
import com.trustvault.model.AccountStatus;
import com.trustvault.model.AuditOutcome;
import com.trustvault.model.User;
import com.trustvault.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(UserResponse::from)
            .toList();
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean updateUserStatus(String targetUserId, AccountStatus newStatus, String adminId, String requestId) {
        Optional<User> targetUser = userRepository.findById(targetUserId);
        if (targetUser.isEmpty()) {
            auditService.logEvent(
                adminId,
                "USER_STATUS_CHANGE",
                null,
                targetUserId,
                AuditOutcome.FAILURE,
                requestId,
                "Target user not found: " + targetUserId
            );
            return false;
        }

        boolean updated = userRepository.updateStatus(targetUserId, newStatus);
        if (updated) {
            auditService.logEvent(
                adminId,
                "USER_STATUS_CHANGE",
                null,
                targetUserId,
                AuditOutcome.SUCCESS,
                requestId,
                "User " + targetUser.get().getEmail() + " status updated to " + newStatus.name()
            );
        }
        return updated;
    }
}
