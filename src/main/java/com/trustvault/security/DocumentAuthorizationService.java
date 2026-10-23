package com.trustvault.security;

import com.trustvault.model.*;
import com.trustvault.repository.DocumentGrantRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DocumentAuthorizationService {

    private final DocumentGrantRepository grantRepository;

    public DocumentAuthorizationService(DocumentGrantRepository grantRepository) {
        this.grantRepository = grantRepository;
    }

    public boolean canRead(User user, Document document) {
        if (user == null || document == null || user.getStatus() != AccountStatus.ACTIVE || document.isDeleted()) {
            return false;
        }

        // Owner always has read permission
        if (document.getOwnerId().equals(user.getId())) {
            return true;
        }

        // Check active grant (READ or EDIT)
        Optional<DocumentGrant> grant = grantRepository.findActiveGrant(document.getId(), user.getId());
        return grant.isPresent() && (grant.get().getPermissionLevel() == PermissionLevel.READ || grant.get().getPermissionLevel() == PermissionLevel.EDIT);
    }

    public boolean canDownload(User user, Document document) {
        return canRead(user, document);
    }

    public boolean canEdit(User user, Document document) {
        if (user == null || document == null || user.getStatus() != AccountStatus.ACTIVE || document.isDeleted()) {
            return false;
        }

        // Owner always has edit permission
        if (document.getOwnerId().equals(user.getId())) {
            return true;
        }

        // Check active grant with EDIT permission
        Optional<DocumentGrant> grant = grantRepository.findActiveGrant(document.getId(), user.getId());
        return grant.isPresent() && grant.get().getPermissionLevel() == PermissionLevel.EDIT;
    }

    public boolean canDelete(User user, Document document) {
        if (user == null || document == null || user.getStatus() != AccountStatus.ACTIVE || document.isDeleted()) {
            return false;
        }

        // Only document owner can delete
        return document.getOwnerId().equals(user.getId());
    }

    public boolean canManageGrants(User user, Document document) {
        if (user == null || document == null || user.getStatus() != AccountStatus.ACTIVE || document.isDeleted()) {
            return false;
        }

        // Only document owner can grant or revoke permissions
        return document.getOwnerId().equals(user.getId());
    }

    public Optional<PermissionLevel> getEffectivePermission(User user, Document document) {
        if (user == null || document == null || user.getStatus() != AccountStatus.ACTIVE || document.isDeleted()) {
            return Optional.empty();
        }

        if (document.getOwnerId().equals(user.getId())) {
            return Optional.of(PermissionLevel.EDIT);
        }

        return grantRepository.findActiveGrant(document.getId(), user.getId())
            .map(DocumentGrant::getPermissionLevel);
    }
}
