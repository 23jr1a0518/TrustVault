package com.trustvault.controller;

import com.trustvault.audit.AuditService;
import com.trustvault.dto.AuditEventResponse;
import com.trustvault.dto.AuditIntegrityReport;
import com.trustvault.dto.UserResponse;
import com.trustvault.dto.UserStatusUpdateRequest;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.security.CorrelationIdFilter;
import com.trustvault.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AuditService auditService;

    public AdminController(UserService userService, AuditService auditService) {
        this.userService = userService;
        this.auditService = auditService;
    }

    private String getRequestId(HttpServletRequest request) {
        String reqId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ID_ATTR);
        return reqId != null ? reqId : "req-admin";
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Map<String, String>> updateUserStatus(
            @PathVariable String id,
            @Valid @RequestBody UserStatusUpdateRequest updateRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        boolean updated = userService.updateUserStatus(id, updateRequest.getStatus(), userDetails.getId(), requestId);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "message", "User status updated to " + updateRequest.getStatus().name()
        ));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditEventResponse>> listAuditEvents(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(auditService.getPaginatedAudit(limit, offset));
    }

    @GetMapping("/audit/integrity")
    public ResponseEntity<AuditIntegrityReport> verifyAuditIntegrity() {
        AuditIntegrityReport report = auditService.verifyChainIntegrity();
        return ResponseEntity.ok(report);
    }
}
