# TrustVault — Enterprise Zero-Trust Document Vault & Audit-Chain Platform

TrustVault is a secure, enterprise-grade document sharing platform built with Java 21, Spring Boot 3, Spring JDBC, and vanilla HTML5/CSS3/JavaScript. It provides fine-grained role-based access control, Insecure Direct Object Reference (IDOR) protection, magic-byte file signature validation, and a mathematically tamper-evident SHA-256 audit hash chain.

---

## 1. Architecture Overview

- **Frontend:** Vanilla HTML5, CSS3, modern JavaScript (ES6+), Bootstrap 5. Served directly by Spring Boot static resources on the same origin (`/index.html`, `/login.html`). No frontend frameworks or server-side template engines used.
- **Backend:** Java 21, Spring Boot 3.3.5, Spring Security, Spring Web, Spring JDBC (`JdbcTemplate` with parameterized queries). No JPA, Hibernate, Flyway, or Liquibase.
- **Database:** PostgreSQL (Supabase project `nxjtgudsmeolyolrgdqu`). Plain SQL schema scripts (`database/schema.sql`, `database/seed-dev.sql`).
- **Storage:** Private Supabase Storage bucket (`trustvault-documents`). Every upload and download is mediated strictly by the backend using server-side credentials.
- **Deployment:** Single container Docker Web Service on Render, binding to `$PORT` on `0.0.0.0`.

---

## 2. Key Security Features

1. **Session Hardening & CSRF Protection:**
   - Server-side `HttpSession` with session ID rotation on login (`request.changeSessionId()`) preventing session fixation.
   - HttpOnly, SameSite=Lax cookies; Secure flag enabled in production.
   - Double-submit CSRF protection using Spring Security's `CookieCsrfTokenRepository` and `X-XSRF-TOKEN` request header.

2. **Zero-Trust Access Delegation:**
   - Centralized `DocumentAuthorizationService` enforcing permissions on every request.
   - Strict separation of roles (`ADMIN`, `OWNER`, `VIEWER`).
   - Granular document permissions (`READ`, `EDIT`).
   - Immediate revocation: revoking an active grant immediately blocks subsequent download and metadata requests.
   - Search isolation: callers can never discover or enumerate documents they do not own or hold active grants for.

3. **Secure File Ingestion:**
   - Strict 20MB file size limit.
   - Path traversal prevention: sanitizes filenames and blocks directory escape sequences (`..`, `/`, `\`).
   - MIME and extension allowlists.
   - Deep magic-byte file signature verification preventing executable disguised files (e.g. `.exe` renamed to `.pdf`).
   - Automatic SHA-256 integrity checksum calculated on byte ingest.
   - Automatic orphaned storage cleanup if metadata persistence fails.

4. **Cryptographic SHA-256 Audit Chain:**
   - Every security-relevant event (logins, uploads, downloads, denied access, grants, revocations, edits) is recorded in an immutable ledger.
   - Each event calculates `current_hash = SHA-256(previous_hash + ":" + deterministic_serialized_event)`.
   - Thread-safe serialization prevents race conditions during concurrent logging.
   - Live admin integrity verification engine (`/api/admin/audit/integrity`) pinpoints the exact event ID if any record is forged or modified in the database.

---

## 3. Environment Variables Reference

Copy `.env.example` to `.env` or set these in Render Environment Settings:

| Variable | Description | Example / Default |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` (or `test`) |
| `PORT` | Listening port (auto-set by Render) | `8080` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://db.nxjtgudsmeolyolrgdqu.supabase.co:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | *(Your Supabase DB password)* |
| `SUPABASE_URL` | Supabase API root URL | `https://nxjtgudsmeolyolrgdqu.supabase.co` |
| `SUPABASE_SECRET_KEY` | Supabase `service_role` secret key | *(Your Supabase service secret)* |
| `SUPABASE_STORAGE_BUCKET` | Private storage bucket name | `trustvault-documents` |
| `SESSION_COOKIE_SECURE` | Enforce HTTPS secure cookie | `true` (prod) / `false` (local dev) |

---

## 4. Database Setup & Initialization

### Supabase Step-by-Step Instructions:

1. Log into your [Supabase Dashboard](https://supabase.com/dashboard/project/nxjtgudsmeolyolrgdqu).
2. Go to **Storage** -> Verify bucket **`trustvault-documents`** exists and is set to **Private** (Public access disabled).
3. Go to **SQL Editor** in the left navigation sidebar.
4. Open and copy the contents of `database/schema.sql` into the query editor, then click **Run**.
5. *(Optional for local/staging test accounts)* Open and run `database/seed-dev.sql` to populate default accounts:
   - **Administrator:** `admin@trustvault.local` / `AdminPass123!`
   - **Document Owner:** `owner@trustvault.local` / `OwnerPass123!`
   - **Viewer:** `viewer@trustvault.local` / `ViewerPass123!`
   - **Disabled User:** `disabled@trustvault.local` / `DisabledPass123!`

---

## 5. Local Build & Test Execution

### Prerequisites:
- Java 21+
- Maven 3.9+ (or use included `./mvnw`)

### Run Tests:
```bash
./mvnw test
```
*Executes all 19 security, authorization, audit-integrity, and file validation tests.*

### Package Executable JAR:
```bash
./mvnw clean package
```

### Run Locally:
```bash
java -jar target/trustvault-1.0.0.jar
```
Navigate to: `http://localhost:8080`

---

## 6. Render Docker Deployment Guide

1. **Push Repository to GitHub:**
   Ensure the latest commits on branch `main` are pushed to `https://github.com/23jr1a0518/TrustVault`.

2. **Create Web Service on Render:**
   - Log into [Render](https://dashboard.render.com/).
   - Click **New +** -> **Web Service**.
   - Connect your GitHub repository: `23jr1a0518/TrustVault`.
   - Choose **Docker** as the Runtime environment.
   - Region: Choose closest to Supabase (e.g. `Oregon (US West)` or `Frankfurt`).
   - Instance Type: `Free` or `Starter`.

3. **Configure Environment Variables on Render:**
   Under **Environment Variables**, add:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `SPRING_DATASOURCE_URL`: `jdbc:postgresql://db.nxjtgudsmeolyolrgdqu.supabase.co:5432/postgres?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME`: `postgres`
   - `SPRING_DATASOURCE_PASSWORD`: *(Your Supabase database password)*
   - `SUPABASE_URL`: `https://nxjtgudsmeolyolrgdqu.supabase.co`
   - `SUPABASE_SECRET_KEY`: *(Your Supabase service_role secret key)*
   - `SUPABASE_STORAGE_BUCKET`: `trustvault-documents`
   - `SESSION_COOKIE_SECURE`: `true`

4. **Deploy & Monitor:**
   - Click **Create Web Service**.
   - Render will build the multi-stage Docker image and launch the Spring Boot container.
   - Access the live application URL: `https://<your-service-name>.onrender.com`.

---

## 7. Security Test Report

### Automated Test Suite Results:
- **Total Tests Run:** 19
- **Failures / Errors:** 0
- **Test Classes:**
  1. `AuthSecurityTest`:
     - Anonymous request rejection (`401 Unauthorized`).
     - Session fixation defense (`changeSessionId` verification).
     - Account lockout enforcement for disabled users (`401 Unauthorized`).
     - Cross-Site Request Forgery (CSRF) token verification on POST endpoints (`403 Forbidden` without token).
     - Role boundary enforcement (`403 Forbidden` for non-admin accessing `/api/admin/**`).
  2. `DocumentAuthorizationTest`:
     - IDOR protection: User B rejected when attempting to read/download User A's document (`403 Forbidden`).
     - Metadata protection: Non-owner/non-editor rejected on edits (`403 Forbidden`).
     - Deletion protection: Non-owner cannot delete document (`403 Forbidden`).
     - Grant lifecycle: Owner grants `READ` to User B -> User B can read/download but cannot edit or grant.
     - Immediate revocation: Owner revokes grant -> User B's immediate next request is rejected (`403 Forbidden`).
     - Search isolation: Documents never leak across users; SQL injection attempts (`' OR '1'='1' --`) safely neutralized.
  3. `AuditIntegrityTest`:
     - Cryptographic SHA-256 hash chaining across sequential events verified.
     - Tampering detection: Direct database modification flagged immediately with `TAMPERING_DETECTED` and exact broken event ID pinpointed.
     - Denied access attempts recorded with `outcome=DENIED` without leaking secrets.
  4. `FileUploadSecurityTest`:
     - Oversized files (> 20MB) rejected immediately.
     - Executable extensions (`.exe`, `.sh`, `.bat`, `.js`, `.jsp`, `.ps1`) blocked.
     - Path traversal attempts (`../../etc/passwd.pdf`) detected and blocked.
     - File signature spoofing (magic byte check) rejected.
     - Legitimate documents with valid headers accepted.

### Vulnerability Simulation Verification:
In branch `test/vulnerability-check`, the authorization check in `DocumentService.getDocumentMetadata` was temporarily bypassed. The security test failed immediately with `AssertionError: Status expected:<403> but was:<200>`. After restoring the check, the test suite passed with 100% success.

---

## 8. Five-Minute Demonstration Checklist

Follow this checklist to demonstrate all platform capabilities during a live review:

- [ ] **Step 1: Sign in as Document Owner**
  - Open `login.html`, click quick-fill button **Owner** (`owner@trustvault.local` / `OwnerPass123!`), and click **Sign In to Vault**.
  - Verify dashboard loads with the active Owner badge.
- [ ] **Step 2: Upload a Confidential Document**
  - Click **+ Upload Document**.
  - Drop a sample file (e.g. `audit_report.pdf`), select category **FINANCE**, and submit.
  - Verify document appears in the table with SHA-256 checksum pill and "Owner" badge.
- [ ] **Step 3: Test Access Grant Delegation**
  - Click **👥 Share** on the uploaded document.
  - Enter recipient email: `viewer@trustvault.local`, select permission **READ**, and click **Grant**.
  - Verify active grant appears in the grants table.
- [ ] **Step 4: Verify Viewer Access & IDOR Enforcement**
  - Sign out, then sign in as **Viewer** (`viewer@trustvault.local` / `ViewerPass123!`).
  - Verify the shared document is visible in the Viewer's vault.
  - Click **⬇ Download** to confirm download works.
  - Observe that "Share" and "Delete" buttons are unavailable.
- [ ] **Step 5: Test Immediate Permission Revocation**
  - Sign back in as **Owner**.
  - Open **👥 Share** on the document and click **Revoke** next to `viewer@trustvault.local`.
  - Sign back in as **Viewer**: verify document is immediately gone from the list, and direct URL access returns `403 Forbidden`.
- [ ] **Step 6: Inspect Cryptographic Audit Ledger & Tamper Engine**
  - Sign in as **Admin** (`admin@trustvault.local` / `AdminPass123!`).
  - Open **Administrator Console** -> Click **🔍 Verify Cryptographic Hash Chain**.
  - Verify status indicates **✔ Chain Intact (Verified)** with total blocks verified.
  - Review recent audit stream entries displaying both `SUCCESS` and intercepted `DENIED` events.
- [ ] **Step 7: Verify Disabled Account Lockout**
  - Under Admin Console **User Directory**, toggle `viewer@trustvault.local` status to **DISABLED**.
  - Attempt logging in as `viewer@trustvault.local`: verify login is rejected with `401 Unauthorized`.
