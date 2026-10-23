# TrustVault REST API Documentation

This document specifies the endpoints, security contracts, HTTP methods, authorization levels, request/response formats, and error codes for the TrustVault platform.

All endpoints use relative URLs originating from the same deployment host.

---

## 1. Authentication & Session Endpoints

### 1.1 User Login
- **Endpoint:** `POST /api/auth/login`
- **Access Level:** Public (Unauthenticated)
- **Description:** Authenticates credentials, rotates the session ID to mitigate session fixation attacks, establishes an `HttpSession`, and records an audit log event.
- **Request Headers:**
  - `Content-Type: application/json`
- **Request Body (`LoginRequest`):**
  ```json
  {
    "email": "owner@trustvault.local",
    "password": "OwnerPass123!"
  }
  ```
- **Responses:**
  - `200 OK`:
    ```json
    {
      "id": "u-owner-00000000-0000-0000-000000000002",
      "email": "owner@trustvault.local",
      "role": "OWNER",
      "status": "ACTIVE",
      "createdAt": "2026-10-23T16:00:00Z"
    }
    ```
  - `401 Unauthorized`: Bad credentials or account disabled (`status == DISABLED`).

---

### 1.2 User Logout
- **Endpoint:** `POST /api/auth/logout`
- **Access Level:** Authenticated
- **Description:** Invalidates the current session, clears the security context, and logs the logout action in the audit trail.
- **Responses:**
  - `200 OK`: `{"message": "Logged out successfully"}`

---

### 1.3 Get Current User Profile
- **Endpoint:** `GET /api/auth/me`
- **Access Level:** Authenticated
- **Description:** Returns the authenticated user's profile and active role.
- **Responses:**
  - `200 OK`: `UserResponse`
  - `401 Unauthorized`: Not authenticated.

---

## 2. Document Management Endpoints

### 2.1 Search & List Documents
- **Endpoint:** `GET /api/documents`
- **Access Level:** Authenticated (`OWNER`, `VIEWER`, `ADMIN`)
- **Query Parameters:**
  - `name` (optional): Filter filename substring.
  - `owner` (optional): Filter owner email.
  - `category` (optional): `FINANCE`, `LEGAL`, `SECURITY`, `HR`, `GENERAL`.
  - `date` (optional): ISO format `YYYY-MM-DD`.
  - `limit` (default `50`): Maximum results.
  - `offset` (default `0`): Pagination offset.
- **Access Isolation Contract:**
  - Callers see ONLY documents they own OR have an active, non-revoked grant for.
  - Unauthorized document metadata or counts are never leaked.
- **Responses:**
  - `200 OK`: Array of `DocumentResponse`

---

### 2.2 Upload Document
- **Endpoint:** `POST /api/documents`
- **Access Level:** Authenticated Active Users (`OWNER`, `ADMIN`)
- **Request Headers:**
  - `Content-Type: multipart/form-data`
  - `X-XSRF-TOKEN: <csrf-token>`
- **Form Data:**
  - `file`: Binary file stream (max 20MB).
  - `category` (optional): `FINANCE`, `LEGAL`, `SECURITY`, `HR`, `GENERAL`.
  - `description` (optional): String (max 2000 chars).
- **Security Validation:**
  - Size check (≤ 20MB).
  - Magic byte verification against extension allowlist.
  - Rejection of executable extensions (`.exe`, `.sh`, `.bat`, `.js`, `.jsp`, etc.).
  - SHA-256 integrity checksum calculated on backend.
  - Storage key generated on server (`vault/<uuid>-<safeFilename>`).
- **Responses:**
  - `201 Created`: `DocumentResponse`
  - `400 Bad Request`: Validation failure or prohibited file type.

---

### 2.3 Get Document Metadata
- **Endpoint:** `GET /api/documents/{id}`
- **Access Level:** Document Owner OR Grantee with Active Grant
- **Responses:**
  - `200 OK`:
    ```json
    {
      "id": "doc-uuid",
      "ownerId": "user-uuid",
      "ownerEmail": "owner@trustvault.local",
      "displayFilename": "financial_audit.pdf",
      "mimeType": "application/pdf",
      "fileSize": 1048576,
      "category": "FINANCE",
      "description": "Quarterly audit review",
      "checksumSha256": "3a7b...",
      "owner": true,
      "permissionLevel": "EDIT",
      "createdAt": "2026-10-23T16:00:00Z",
      "updatedAt": "2026-10-23T16:00:00Z"
    }
    ```
  - `403 Forbidden`: Caller lacks read access (logged to audit as `DENIED`).
  - `404 Not Found`: Document ID does not exist.

---

### 2.4 Update Document Metadata
- **Endpoint:** `PATCH /api/documents/{id}`
- **Access Level:** Document Owner OR Grantee with `EDIT` Permission
- **Request Headers:**
  - `Content-Type: application/json`
  - `X-XSRF-TOKEN: <csrf-token>`
- **Request Body (`DocumentUpdateRequest`):**
  ```json
  {
    "displayFilename": "new_filename.pdf",
    "category": "LEGAL",
    "description": "Updated document notes"
  }
  ```
- **Responses:**
  - `200 OK`: Updated `DocumentResponse`
  - `403 Forbidden`: Insufficient permissions.

---

### 2.5 Delete Document
- **Endpoint:** `DELETE /api/documents/{id}`
- **Access Level:** Document Owner Only
- **Request Headers:**
  - `X-XSRF-TOKEN: <csrf-token>`
- **Responses:**
  - `200 OK`: `{"message": "Document deleted successfully"}`
  - `403 Forbidden`: Attempted deletion by non-owner.

---

### 2.6 Download Document Stream
- **Endpoint:** `GET /api/documents/{id}/download`
- **Access Level:** Document Owner OR Grantee with Active `READ` or `EDIT` Grant
- **Response Headers:**
  - `Content-Disposition: attachment; filename="<safe-filename>"`
  - `Content-Type: <mime-type>`
  - `Cache-Control: no-store, no-cache, must-revalidate, max-age=0`
  - `X-Content-Type-Options: nosniff`
- **Responses:**
  - `200 OK`: Binary octet stream
  - `403 Forbidden`: Unauthorized download attempt (audited with `outcome=DENIED`).

---

## 3. Sharing & Grant Delegation Endpoints

### 3.1 List Grants for Document
- **Endpoint:** `GET /api/documents/{id}/grants`
- **Access Level:** Document Owner Only
- **Responses:**
  - `200 OK`: Array of `GrantResponse`

---

### 3.2 Create Access Grant
- **Endpoint:** `POST /api/documents/{id}/grants`
- **Access Level:** Document Owner Only
- **Request Body (`CreateGrantRequest`):**
  ```json
  {
    "recipientEmail": "colleague@trustvault.local",
    "permissionLevel": "READ"
  }
  ```
- **Responses:**
  - `201 Created`: `GrantResponse`
  - `400 Bad Request`: Recipient does not exist or is disabled.
  - `403 Forbidden`: Non-owner attempted to grant access.

---

### 3.3 Revoke Access Grant
- **Endpoint:** `DELETE /api/documents/{id}/grants/{grantId}`
- **Access Level:** Document Owner Only
- **Responses:**
  - `200 OK`: `{"message": "Permission grant revoked successfully"}`
  - `403 Forbidden`: Non-owner attempted to revoke grant.

---

### 3.4 Document-Specific Audit History
- **Endpoint:** `GET /api/documents/{id}/audit`
- **Access Level:** Authorized Document Users
- **Responses:**
  - `200 OK`: Array of `AuditEventResponse`

---

## 4. Administrator & Audit Integrity Endpoints

### 4.1 System Audit Stream
- **Endpoint:** `GET /api/admin/audit`
- **Access Level:** `ADMIN` Role Only (`ROLE_ADMIN`)
- **Query Parameters:**
  - `limit` (default `100`), `offset` (default `0`)
- **Responses:**
  - `200 OK`: Array of `AuditEventResponse`
  - `403 Forbidden`: Non-admin caller.

---

### 4.2 Cryptographic Audit Chain Verification
- **Endpoint:** `GET /api/admin/audit/integrity`
- **Access Level:** `ADMIN` Role Only (`ROLE_ADMIN`)
- **Responses:**
  - `200 OK` (Chain Intact):
    ```json
    {
      "valid": true,
      "totalEventsVerified": 42,
      "status": "CHAIN_INTACT",
      "brokenAtEventId": null,
      "calculatedHash": null,
      "recordedHash": null,
      "latestHash": "0f26b623e8d0467ded008997fd2ff57e2265ed098951eb80825d78f33907d8a0",
      "verificationTimestamp": "2026-10-23T16:20:00Z"
    }
    ```
  - `200 OK` (Tampering Detected):
    ```json
    {
      "valid": false,
      "totalEventsVerified": 42,
      "status": "TAMPERING_DETECTED",
      "brokenAtEventId": "ae-uuid-tampered",
      "calculatedHash": "calculated-sha256",
      "recordedHash": "recorded-sha256",
      "latestHash": null,
      "verificationTimestamp": "2026-10-23T16:20:00Z"
    }
    ```

---

### 4.3 List All Users
- **Endpoint:** `GET /api/admin/users`
- **Access Level:** `ADMIN` Role Only
- **Responses:**
  - `200 OK`: Array of `UserResponse`

---

### 4.4 Update User Account Status
- **Endpoint:** `PATCH /api/admin/users/{id}/status`
- **Access Level:** `ADMIN` Role Only
- **Request Body (`UserStatusUpdateRequest`):**
  ```json
  {
    "status": "DISABLED"
  }
  ```
- **Responses:**
  - `200 OK`: `{"message": "User status updated to DISABLED"}`
  - `404 Not Found`: Target user does not exist.

---

## 5. Health Monitoring

### 5.1 System Health
- **Endpoint:** `GET /api/health`
- **Access Level:** Public
- **Responses:**
  - `200 OK`:
    ```json
    {
      "application": "TrustVault",
      "version": "1.0.0",
      "status": "UP",
      "database": "UP",
      "timestamp": "2026-10-23T16:30:00Z"
    }
    ```
