package com.trustvault.service;

import com.trustvault.audit.AuditService;
import com.trustvault.dto.CreateGrantRequest;
import com.trustvault.dto.DocumentResponse;
import com.trustvault.dto.DocumentUpdateRequest;
import com.trustvault.dto.GrantResponse;
import com.trustvault.model.*;
import com.trustvault.repository.DocumentGrantRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import com.trustvault.security.DocumentAuthorizationService;
import com.trustvault.storage.FileValidationService;
import com.trustvault.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentGrantRepository grantRepository;
    private final UserRepository userRepository;
    private final DocumentAuthorizationService authService;
    private final FileValidationService fileValidationService;
    private final StorageService storageService;
    private final AuditService auditService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentGrantRepository grantRepository,
                           UserRepository userRepository,
                           DocumentAuthorizationService authService,
                           FileValidationService fileValidationService,
                           StorageService storageService,
                           AuditService auditService) {
        this.documentRepository = documentRepository;
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.fileValidationService = fileValidationService;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    public DocumentResponse uploadDocument(MultipartFile file, String category, String description,
                                           User currentUser, String requestId) {
        // Enforce active account status
        if (currentUser.getStatus() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException("Disabled account cannot upload documents");
        }

        // Validate file integrity, size, extensions, magic bytes
        fileValidationService.validateUpload(file);

        String originalFilename = file.getOriginalFilename();
        String safeDisplayFilename = fileValidationService.sanitizeFilename(originalFilename);
        String resolvedMimeType = fileValidationService.resolveMimeType(safeDisplayFilename, file.getContentType());
        String documentId = "doc-" + UUID.randomUUID().toString();
        String storageKey = "vault/" + UUID.randomUUID().toString() + "-" + safeDisplayFilename;

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read upload payload: " + e.getMessage(), e);
        }

        String checksum = fileValidationService.computeSha256(fileBytes);
        String finalCategory = (category != null && !category.isBlank()) ? category.trim() : "GENERAL";

        Document document = new Document(
            documentId,
            currentUser.getId(),
            safeDisplayFilename,
            storageKey,
            resolvedMimeType,
            file.getSize(),
            finalCategory,
            description,
            checksum,
            false,
            Instant.now(),
            Instant.now()
        );

        // Upload to private storage
        storageService.uploadObject(storageKey, fileBytes, resolvedMimeType);

        try {
            documentRepository.save(document);
        } catch (Exception e) {
            log.error("Database save failed for document {}. Cleaning up orphaned storage object.", documentId, e);
            storageService.deleteObject(storageKey);
            throw new IllegalStateException("Failed to persist document metadata: " + e.getMessage(), e);
        }

        auditService.logEvent(
            currentUser.getId(),
            "DOCUMENT_UPLOAD",
            documentId,
            null,
            AuditOutcome.SUCCESS,
            requestId,
            String.format("Uploaded '%s' (%d bytes, SHA-256: %s)", safeDisplayFilename, file.getSize(), checksum)
        );

        return DocumentResponse.from(document, currentUser.getEmail(), true, PermissionLevel.EDIT);
    }

    public DocumentResponse getDocumentMetadata(String documentId, User currentUser, String requestId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!authService.canRead(currentUser, document)) {
            auditService.logEvent(
                currentUser.getId(),
                "DOCUMENT_METADATA_READ",
                documentId,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Unauthorized attempt to inspect document metadata"
            );
            throw new AccessDeniedException("You do not have permission to access document: " + documentId);
        }

        String ownerEmail = userRepository.findById(document.getOwnerId())
            .map(User::getEmail)
            .orElse("unknown");

        boolean isOwner = document.getOwnerId().equals(currentUser.getId());
        PermissionLevel permission = authService.getEffectivePermission(currentUser, document).orElse(null);

        return DocumentResponse.from(document, ownerEmail, isOwner, permission);
    }

    public InputStream downloadDocumentStream(String documentId, User currentUser, String requestId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!authService.canDownload(currentUser, document)) {
            auditService.logEvent(
                currentUser.getId(),
                "DOCUMENT_DOWNLOAD",
                documentId,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Unauthorized attempt to download document"
            );
            throw new AccessDeniedException("You do not have permission to download document: " + documentId);
        }

        InputStream stream = storageService.downloadObject(document.getStorageObjectKey());

        auditService.logEvent(
            currentUser.getId(),
            "DOCUMENT_DOWNLOAD",
            documentId,
            null,
            AuditOutcome.SUCCESS,
            requestId,
            String.format("Downloaded '%s'", document.getDisplayFilename())
        );

        return stream;
    }

    public Document getDocumentForDownload(String documentId, User currentUser, String requestId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!authService.canDownload(currentUser, document)) {
            auditService.logEvent(
                currentUser.getId(),
                "DOCUMENT_DOWNLOAD",
                documentId,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Unauthorized download attempt"
            );
            throw new AccessDeniedException("Access denied for download: " + documentId);
        }

        return document;
    }

    public DocumentResponse updateMetadata(String documentId, DocumentUpdateRequest request,
                                           User currentUser, String requestId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!authService.canEdit(currentUser, document)) {
            auditService.logEvent(
                currentUser.getId(),
                "DOCUMENT_EDIT",
                documentId,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Unauthorized attempt to update document metadata"
            );
            throw new AccessDeniedException("You do not have permission to edit document: " + documentId);
        }

        String safeFilename = request.getDisplayFilename() != null && !request.getDisplayFilename().isBlank()
            ? fileValidationService.sanitizeFilename(request.getDisplayFilename())
            : document.getDisplayFilename();

        documentRepository.updateMetadata(documentId, safeFilename, request.getCategory(), request.getDescription());

        auditService.logEvent(
            currentUser.getId(),
            "DOCUMENT_EDIT",
            documentId,
            null,
            AuditOutcome.SUCCESS,
            requestId,
            String.format("Updated metadata: filename='%s', category='%s'", safeFilename, request.getCategory())
        );

        return getDocumentMetadata(documentId, currentUser, requestId);
    }

    public void deleteDocument(String documentId, User currentUser, String requestId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!authService.canDelete(currentUser, document)) {
            auditService.logEvent(
                currentUser.getId(),
                "DOCUMENT_DELETE",
                documentId,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Unauthorized attempt to delete document"
            );
            throw new AccessDeniedException("Only the document owner can delete document: " + documentId);
        }

        documentRepository.softDelete(documentId);

        auditService.logEvent(
            currentUser.getId(),
            "DOCUMENT_DELETE",
            documentId,
            null,
            AuditOutcome.SUCCESS,
            requestId,
            String.format("Soft-deleted document '%s'", document.getDisplayFilename())
        );
    }

    public GrantResponse grantAccess(String documentId, CreateGrantRequest request,
                                     User currentUser, String requestId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!authService.canManageGrants(currentUser, document)) {
            auditService.logEvent(
                currentUser.getId(),
                "GRANT_ACCESS",
                documentId,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Unauthorized attempt to grant document permissions"
            );
            throw new AccessDeniedException("Only the document owner can grant permissions: " + documentId);
        }

        User recipient = userRepository.findByEmail(request.getRecipientEmail())
            .orElseThrow(() -> new IllegalArgumentException("Recipient not found with email: " + request.getRecipientEmail()));

        if (recipient.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot grant permissions to yourself");
        }

        if (recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot grant permissions to a disabled user account");
        }

        // If active grant exists, revoke old grant first to preserve history and prevent duplicates
        Optional<DocumentGrant> existing = grantRepository.findActiveGrant(documentId, recipient.getId());
        existing.ifPresent(oldGrant -> grantRepository.revokeGrant(oldGrant.getId(), currentUser.getId()));

        String grantId = "grant-" + UUID.randomUUID().toString();
        Instant expiresAt = (request.getDurationHours() != null && request.getDurationHours() > 0)
            ? Instant.now().plus(java.time.Duration.ofHours(request.getDurationHours()))
            : null;

        DocumentGrant newGrant = new DocumentGrant(
            grantId,
            documentId,
            recipient.getId(),
            request.getPermissionLevel(),
            currentUser.getId(),
            Instant.now(),
            expiresAt,
            null,
            null
        );

        grantRepository.save(newGrant);

        String grantAuditDetail = expiresAt != null
            ? String.format("Granted %s permission to %s (Expires in %d hours)", request.getPermissionLevel(), recipient.getEmail(), request.getDurationHours())
            : String.format("Granted %s permission to %s", request.getPermissionLevel(), recipient.getEmail());

        auditService.logEvent(
            currentUser.getId(),
            "GRANT_ACCESS",
            documentId,
            recipient.getId(),
            AuditOutcome.SUCCESS,
            requestId,
            grantAuditDetail
        );

        return GrantResponse.from(newGrant, recipient.getEmail(), currentUser.getEmail(), null);
    }

    public void revokeGrant(String documentId, String grantId, User currentUser, String requestId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!authService.canManageGrants(currentUser, document)) {
            auditService.logEvent(
                currentUser.getId(),
                "REVOKE_ACCESS",
                documentId,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Unauthorized attempt to revoke document permission"
            );
            throw new AccessDeniedException("Only the document owner can revoke permissions: " + documentId);
        }

        DocumentGrant grant = grantRepository.findById(grantId)
            .orElseThrow(() -> new IllegalArgumentException("Grant record not found: " + grantId));

        if (!grant.getDocumentId().equals(documentId)) {
            throw new IllegalArgumentException("Grant does not belong to specified document");
        }

        if (grant.isActive()) {
            grantRepository.revokeGrant(grantId, currentUser.getId());

            auditService.logEvent(
                currentUser.getId(),
                "REVOKE_ACCESS",
                documentId,
                grant.getRecipientId(),
                AuditOutcome.SUCCESS,
                requestId,
                "Revoked permission grant: " + grantId
            );
        }
    }

    public List<GrantResponse> listDocumentGrants(String documentId, User currentUser, String requestId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!authService.canManageGrants(currentUser, document)) {
            auditService.logEvent(
                currentUser.getId(),
                "LIST_GRANTS",
                documentId,
                null,
                AuditOutcome.DENIED,
                requestId,
                "Unauthorized attempt to list document grants"
            );
            throw new AccessDeniedException("Only the document owner can view sharing grants: " + documentId);
        }

        return grantRepository.findGrantResponsesByDocumentId(documentId);
    }

    public List<DocumentResponse> searchDocuments(String name, String owner, String category,
                                                  LocalDate date, int limit, int offset,
                                                  User currentUser) {
        return documentRepository.searchAccessibleDocuments(
            currentUser.getId(), name, owner, category, date, limit, offset
        );
    }
}
