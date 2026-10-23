# TrustVault Security Threat Model & Defense-in-Depth Analysis

This document details the threat model, attack vectors, mitigations, security boundaries, and residual risks for the TrustVault confidential document sharing platform.

---

## 1. System Architecture & Trust Boundaries

TrustVault defines four distinct security trust boundaries:

```
[ Browser Client ]
       │  (Untrusted boundary: HTTPS, SameSite Cookies, CSRF Headers)
       ▼
[ Spring Boot Application Server (Port 8080) ]
       │  (Trusted Core: Centralized DocumentAuthorizationService, SHA-256 Hashing)
       ├──► [ PostgreSQL Database (Supabase) ] (Parameterized JDBC, Schema Constraints)
       └──► [ Private Object Storage (Supabase) ] (Backend-Mediation via Secret Role Key)
```

1. **Client / Browser (Untrusted):**
   - The frontend JavaScript execution environment is treated as completely untrusted.
   - No privileged service credentials (`SUPABASE_SECRET_KEY`) or direct storage links are ever visible in browser context or frontend code.
   - Hiding UI buttons is treated strictly as a UX affordance; every protected route is independently validated on the Java backend.

2. **Application Server (Trusted Execution Core):**
   - Java 21 / Spring Boot 3 running as an unprivileged Linux user (`trustvault`) inside a minimal Docker container.
   - Mediates all data and file interactions.

3. **Data Layer (PostgreSQL):**
   - Bound via SSL connection (`sslmode=require`).
   - Protected against injection via Spring JDBC parameterized queries (`?`).

4. **Private Storage Layer (Supabase Storage):**
   - Private bucket (`trustvault-documents`) with public read access disabled.
   - Accessible strictly through server-side authenticated REST API calls.

---

## 2. STRIDE Threat Analysis & Mitigations

### 2.1 Spoofing (Identity Impersonation)
- **Threat:** An attacker steals an active session ID or conducts session fixation before authentication.
- **Mitigation:**
  - Session fixation protection: `request.changeSessionId()` rotates the session identifier immediately upon successful login.
  - HttpOnly cookies prevent script-based cookie theft (XSS defense).
  - Production cookie hardening: `Secure` flag enforced when deployed over HTTPS; `SameSite=Lax` prevents cross-site request inclusion.
  - Password security: BCrypt hashing with a cost factor of 10.

### 2.2 Tampering (Data Modification)
- **Threat 1: File Alteration & Executable Uploads:**
  - An attacker uploads executable binaries (`.exe`, `.jsp`, `.sh`, `.js`) disguised as PDF or images.
  - **Mitigation:** `FileValidationService` performs dual-layer validation:
    1. Extension allowlist check against prohibited executable types.
    2. Magic byte header inspection verifying file signatures (`%PDF`, PNG, JPEG, ZIP/DOCX).
    3. Filename sanitization stripping path traversal sequences (`..`, `/`, `\`).
    4. Cryptographic SHA-256 checksum calculated on ingest and persisted in DB.
- **Threat 2: Audit Trail Manipulation:**
  - An attacker or rogue user attempts to modify or delete audit log entries to cover tracks.
  - **Mitigation:**
    - Cryptographic SHA-256 hash chaining: every event links to `previous_hash` via deterministic serialization.
    - Application endpoints only append; no update or delete endpoints exist for audit records.
    - `AuditService.verifyChainIntegrity()` mathematically verifies the unbroken sequence from genesis.

### 2.3 Repudiation (Denial of Actions)
- **Threat:** A user claims they never downloaded, edited, or granted access to a confidential file.
- **Mitigation:**
  - Every security-relevant action (upload, download, metadata edit, grant, revocation, delete, failed login) generates an immutable audit record.
  - Unique correlation ID (`X-Request-ID`) traces actions from HTTP request to audit log.
  - Fail-secure: If the audit recording transaction fails, the operation is aborted and rolled back.

### 2.4 Information Disclosure (Confidentiality Breach)
- **Threat 1: Insecure Direct Object References (IDOR):**
  - User A inspects or downloads User B's document by guessing UUIDs.
  - **Mitigation:** Centralized `DocumentAuthorizationService` validates ownership or active grant record on every single request. Direct UUID access without an active grant returns `403 Forbidden` and logs an audit event with `outcome=DENIED`.
- **Threat 2: Search Metadata Leakage:**
  - An unauthorized user discovers the existence or count of confidential documents via search queries.
  - **Mitigation:** Search SQL query (`searchAccessibleDocuments`) enforces caller isolation at the database level (`d.owner_id = ? OR dg.id IS NOT NULL`). Unauthorized document names, IDs, or counts never enter the query result set.
- **Threat 3: Browser Caching of Sensitive Files:**
  - **Mitigation:** File download responses include `Cache-Control: no-store, no-cache, must-revalidate, max-age=0` and `X-Content-Type-Options: nosniff`.

### 2.5 Denial of Service (Resource Exhaustion)
- **Threat:** Uploading massive files to exhaust server memory or disk storage.
- **Mitigation:**
  - Hard limit of 20MB enforced in both Spring multipart configuration and `FileValidationService`.
  - Memory bounds configured with container support `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`.

### 2.6 Elevation of Privilege
- **Threat:** Viewers attempt to grant permissions or access administrative endpoints.
- **Mitigation:**
  - Spring Security `@PreAuthorize("hasRole('ADMIN')")` strictly protects `/api/admin/**`.
  - `DocumentAuthorizationService.canManageGrants` ensures only the document owner can issue or revoke grants.
  - Immediate revocation: When an owner revokes a grant, the next request from the recipient fails immediately with `403 Forbidden`.

---

## 3. Residual Risks & Operational Boundaries

1. **Privileged Database Superuser Threat:**
   - *Limitation:* A database administrator or attacker with direct `postgres` superuser access could theoretically modify both the audit records and recalculate all hashes down the chain.
   - *Mitigation in Production:* Restrict direct database access to trusted operations; stream audit logs or periodic anchor hashes to an external, write-once/immutable append-only log or cold storage archive.

2. **Supabase Storage Direct Access:**
   - *Limitation:* If an administrator accidentally makes the `trustvault-documents` bucket public in the Supabase Dashboard, files could be downloaded anonymously.
   - *Mitigation:* The bucket MUST be configured as **Private** in Supabase Storage settings.
