package com.trustvault.controller;

import com.trustvault.audit.AuditService;
import com.trustvault.dto.LoginRequest;
import com.trustvault.dto.UserResponse;
import com.trustvault.model.AuditOutcome;
import com.trustvault.model.User;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.security.CorrelationIdFilter;
import com.trustvault.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.auditService = auditService;
    }

    private String getRequestId(HttpServletRequest request) {
        String reqId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ID_ATTR);
        return reqId != null ? reqId : "req-auth";
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String requestId = getRequestId(request);
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail().trim(), loginRequest.getPassword())
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // Rotate session ID to protect against session fixation attacks
            request.getSession(true);
            request.changeSessionId();
            HttpSession session = request.getSession();
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            auditService.logEvent(
                user.getId(),
                "USER_LOGIN",
                null,
                null,
                AuditOutcome.SUCCESS,
                requestId,
                "Successful login for user: " + user.getEmail()
            );

            return ResponseEntity.ok(UserResponse.from(user));
        } catch (AuthenticationException ex) {
            auditService.logEvent(
                null,
                "USER_LOGIN",
                null,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Failed login attempt for: " + loginRequest.getEmail() + " (" + ex.getMessage() + ")"
            );
            throw new BadCredentialsException("Invalid email or password", ex);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        String requestId = getRequestId(request);
        String actorId = userDetails != null ? userDetails.getId() : null;

        auditService.logEvent(
            actorId,
            "USER_LOGOUT",
            null,
            null,
            AuditOutcome.SUCCESS,
            requestId,
            "User logged out"
        );

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findById(userDetails.getId())
            .orElse(userDetails.getUser());

        return ResponseEntity.ok(UserResponse.from(user));
    }
}
