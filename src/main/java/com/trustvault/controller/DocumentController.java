package com.trustvault.controller;

import com.trustvault.audit.AuditService;
import com.trustvault.dto.AuditEventResponse;
import com.trustvault.dto.CreateGrantRequest;
import com.trustvault.dto.DocumentResponse;
import com.trustvault.dto.DocumentUpdateRequest;
import com.trustvault.dto.GrantResponse;
import com.trustvault.model.Document;
import com.trustvault.model.User;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.security.CorrelationIdFilter;
import com.trustvault.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AuditService auditService;

    public DocumentController(DocumentService documentService, AuditService auditService) {
        this.documentService = documentService;
        this.auditService = auditService;
    }

    private String getRequestId(HttpServletRequest request) {
        String reqId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ID_ATTR);
        return reqId != null ? reqId : "req-doc";
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> searchDocuments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        List<DocumentResponse> results = documentService.searchDocuments(
            name, owner, category, date, limit, offset, currentUser
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        User currentUser = userDetails.getUser();
        DocumentResponse response = documentService.uploadDocument(file, category, description, currentUser, requestId);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentMetadata(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        DocumentResponse response = documentService.getDocumentMetadata(id, userDetails.getUser(), requestId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateMetadata(
            @PathVariable String id,
            @Valid @RequestBody DocumentUpdateRequest updateRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        DocumentResponse response = documentService.updateMetadata(id, updateRequest, userDetails.getUser(), requestId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        documentService.deleteDocument(id, userDetails.getUser(), requestId);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        Document doc = documentService.getDocumentForDownload(id, userDetails.getUser(), requestId);
        InputStream stream = documentService.downloadDocumentStream(id, userDetails.getUser(), requestId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(doc.getMimeType()));
        headers.setContentLength(doc.getFileSize());
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getDisplayFilename() + "\"");
        headers.set(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        headers.set("Pragma", "no-cache");
        headers.set("X-Content-Type-Options", "nosniff");

        return ResponseEntity.ok()
            .headers(headers)
            .body(new InputStreamResource(stream));
    }

    @GetMapping("/{id}/grants")
    public ResponseEntity<List<GrantResponse>> listGrants(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        List<GrantResponse> grants = documentService.listDocumentGrants(id, userDetails.getUser(), requestId);
        return ResponseEntity.ok(grants);
    }

    @PostMapping("/{id}/grants")
    public ResponseEntity<GrantResponse> createGrant(
            @PathVariable String id,
            @Valid @RequestBody CreateGrantRequest grantRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        GrantResponse grant = documentService.grantAccess(id, grantRequest, userDetails.getUser(), requestId);
        return ResponseEntity.status(201).body(grant);
    }

    @DeleteMapping("/{id}/grants/{grantId}")
    public ResponseEntity<Map<String, String>> revokeGrant(
            @PathVariable String id,
            @PathVariable String grantId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        documentService.revokeGrant(id, grantId, userDetails.getUser(), requestId);
        return ResponseEntity.ok(Map.of("message", "Permission grant revoked successfully"));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditEventResponse>> getDocumentAudit(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String requestId = getRequestId(request);
        // Ensure user has at least read access to document
        documentService.getDocumentMetadata(id, userDetails.getUser(), requestId);
        List<AuditEventResponse> history = auditService.getDocumentAudit(id, limit, offset);
        return ResponseEntity.ok(history);
    }
}
