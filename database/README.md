# TrustVault Database Architecture & Management Guide

This document describes the schema management, script execution order, and security considerations for the TrustVault PostgreSQL database on Supabase.

## 1. Schema Overview

TrustVault uses standard relational PostgreSQL tables with strict constraints, foreign keys, and indexes. No ORM or migration framework (e.g. Hibernate/Flyway/Liquibase) is permitted; all data access is managed via Spring JDBC `JdbcTemplate` with parameterized queries.

### Tables

1. **`users`**
   - Stores user identities, authentication credentials (BCrypt hashed), authorization role (`ADMIN`, `OWNER`, `VIEWER`), and account status (`ACTIVE`, `DISABLED`).
   - Indexes on `email`, `role`, and `status`.

2. **`documents`**
   - Stores metadata for uploaded documents: owner ID, display filename, server-generated private storage object key (UUID-based), MIME type, byte size, category, description, and computed SHA-256 integrity checksum.
   - Soft-deletion flag (`is_deleted`) to preserve audit and history.

3. **`document_grants`**
   - Controls document-level permission delegation (`READ`, `EDIT`).
   - Tracks granting user, grant timestamp, revocation timestamp, and revoking user.
   - Enforces business logic: **A partial unique index (`idx_unique_active_grant`) ensures that at most one active (non-revoked) grant exists per `(document_id, recipient_id)` pair**.

4. **`audit_events`**
   - High-integrity, tamper-evident security audit log.
   - Implements a cryptographic SHA-256 hash chain: each record includes `previous_hash` and `current_hash = SHA-256(previous_hash + ":" + deterministic_serialized_event)`.
   - Records actor ID, action type, document ID, target user ID, outcome (`SUCCESS`, `DENIED`, `FAILURE`), timestamp, request correlation ID, and sanitized event details.

---

## 2. Execution Order

When setting up a new environment or configuring the Supabase project (`nxjtgudsmeolyolrgdqu`), run the scripts in the following order:

1. **`database/schema.sql`**
   - Creates the core tables, foreign keys, indexes, and partial unique constraint.
   - Safe and idempotent (`CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`).

2. **`database/seed-dev.sql`** *(Development / Staging Only)*
   - Seeds baseline users (`admin@trustvault.local`, `owner@trustvault.local`, `viewer@trustvault.local`, `disabled@trustvault.local`) and the audit chain genesis block.
   - Safe and idempotent (`ON CONFLICT (id) DO NOTHING`).

---

## 3. How to Apply in Supabase

1. Open the [Supabase Dashboard for project `nxjtgudsmeolyolrgdqu`](https://supabase.com/dashboard/project/nxjtgudsmeolyolrgdqu).
2. Navigate to **SQL Editor** in the left navigation sidebar.
3. Click **New Query**.
4. Paste the contents of `database/schema.sql` and click **Run**.
5. (Optional for initial test users) Open another query, paste `database/seed-dev.sql`, and click **Run**.
6. Verify in the **Table Editor** that `users`, `documents`, `document_grants`, and `audit_events` appear with correct columns.

---

## 4. Security Principles

- **No Destructive Drops:** DDL scripts avoid `DROP TABLE` or `TRUNCATE` in production.
- **Parameterized SQL:** All Java queries execute via `JdbcTemplate` with parameterized placeholders (`?`), completely mitigating SQL injection risks.
- **Principle of Least Privilege:** Application endpoints never receive direct table-drop or DDL permissions.
- **Audit Tamper Evidence:** Changes to past audit logs invalidate the cryptographic hash chain, immediately reported by the `/api/admin/audit/integrity` service.
