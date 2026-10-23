# TrustVault Engineering & Security Prompts Log

This document records the dated AI engineering prompt interactions used during the development, security hardening, and deployment preparation of TrustVault.

---

### Entry 1: Architecture Inception & Zero-Trust Foundation
- **Timestamp:** `2026-10-23T21:34:21+05:30`
- **Context:** Initial project briefing and stack selection.
- **Prompt:**
  > "Act as a senior Java full-stack developer, application security engineer, QA engineer, and deployment engineer. Build a complete, secure document-sharing application called TrustVault. Inspect the existing repository, implement the project, run tests, fix issues, and guide me through deployment until the live application works. Stack constraints: Vanilla JS/HTML/CSS (no frontend frameworks), Java 21, Spring Boot 3, Spring JDBC (JdbcTemplate only, no ORM), Supabase PostgreSQL, private Supabase Storage, and Render Docker Web Service deployment."
- **Key Decision & Outcome:**
  - Initialized Spring Boot 3.3.5 with Spring Security, Spring JDBC, PostgreSQL driver, and Actuator.
  - Avoided JPA/Hibernate overhead in favor of pure parameterized SQL queries with `JdbcTemplate`.

---

### Entry 2: Database Schema & Partial Unique Constraints
- **Timestamp:** `2026-10-23T21:44:00+05:30`
- **Context:** Designing the PostgreSQL relational schema and ensuring data integrity without ORMs.
- **Prompt:**
  > "Design a plain SQL database schema in `database/schema.sql` for users, documents, document grants, and audit events. Ensure that document grants preserve revoked grant history while preventing duplicate active grants using partial indexing. Ensure all statements are idempotent and safe to run in a documented order."
- **Key Decision & Outcome:**
  - Created `database/schema.sql` with `CREATE UNIQUE INDEX idx_unique_active_grant ON document_grants (document_id, recipient_id) WHERE revocation_timestamp IS NULL;`.
  - Used portable ANSI SQL `if-exists` query patterns in Spring repositories for seamless H2 testing and production PostgreSQL execution.

---

### Entry 3: Centralized Document Authorization & Immediate Revocation (Auth Focus)
- **Timestamp:** `2026-10-23T21:52:00+05:30`
- **Context:** Eliminating IDOR vulnerabilities and enforcing backend-only authorization controls.
- **Prompt:**
  > "Implement a centralized `DocumentAuthorizationService` enforcing strict zero-trust rules: owners manage their own documents; viewers access only documents shared with them; `READ` permits reading and downloading; `EDIT` permits metadata updates but not sharing or deletion; administrators manage admin features but do not automatically bypass document confidentiality; disabled accounts lose access; and revocation immediately blocks subsequent requests."
- **Key Decision & Outcome:**
  - Implemented `DocumentAuthorizationService` with explicit checks on user status (`ACTIVE`), ownership, and active grants.
  - Every unauthorized access attempt is intercepted, throws Spring `AccessDeniedException`, and records an audit log with `outcome = 'DENIED'`.

---

### Entry 4: Cryptographic SHA-256 Hash Chaining & Tamper Detection (Audit Focus)
- **Timestamp:** `2026-10-23T21:54:30+05:30`
- **Context:** Building tamper-evident audit logging with mathematical integrity verification.
- **Prompt:**
  > "Implement an immutable audit trail using a SHA-256 hash chain with deterministic event serialization. Each record must compute `current_hash = SHA-256(previous_hash + ':' + serialized_fields)`. Provide a thread-safe audit logging service and an integrity verification engine that scans the chain and identifies the exact event ID where tampering occurred."
- **Key Decision & Outcome:**
  - Designed `AuditService` using a reentrant lock to serialize hash calculations across concurrent requests.
  - Built `verifyChainIntegrity()` which recalculates hashes from genesis to head; validated in `AuditIntegrityTest` by modifying a database record directly and confirming `TAMPERING_DETECTED`.

---

### Entry 5: Security Regression Testing & Intentional Vulnerability Injection
- **Timestamp:** `2026-10-23T22:17:30+05:30`
- **Context:** Proving test sensitivity by verifying that security tests fail when defenses are removed.
- **Prompt:**
  > "Temporarily remove an authorization check in a local-only test branch, verify that its security test fails, restore the check, and rerun the test. Never deploy the vulnerable version. Report real test results only."
- **Key Decision & Outcome:**
  - Created branch `test/vulnerability-check` and commented out `authService.canRead()` in `DocumentService.getDocumentMetadata()`.
  - Ran `mvn test -Dtest=DocumentAuthorizationTest#testUserBCannotReadUserADocument`, producing real failure: `java.lang.AssertionError: Status expected:<403> but was:<200>`.
  - Restored the check, verified that all 19 tests passed (`BUILD SUCCESS`), and safely deleted the test branch.
