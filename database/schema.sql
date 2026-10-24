-- ==============================================================================
-- TRUSTVAULT DATABASE SCHEMA
-- Target Database: Supabase PostgreSQL / Standard PostgreSQL 15+
-- Safe execution: Idempotent with IF NOT EXISTS and non-destructive modifications.
-- ==============================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('ADMIN', 'OWNER', 'VIEWER')),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- 2. DOCUMENTS TABLE
CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_filename VARCHAR(255) NOT NULL,
    storage_object_key VARCHAR(255) NOT NULL UNIQUE,
    mime_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
    description TEXT,
    checksum_sha256 VARCHAR(64) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_owner ON documents(owner_id);
CREATE INDEX IF NOT EXISTS idx_documents_deleted ON documents(is_deleted);
CREATE INDEX IF NOT EXISTS idx_documents_category ON documents(category);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_documents_filename ON documents(display_filename);

-- 3. DOCUMENT GRANTS TABLE
CREATE TABLE IF NOT EXISTS document_grants (
    id VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    recipient_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_level VARCHAR(16) NOT NULL CHECK (permission_level IN ('READ', 'EDIT')),
    granted_by VARCHAR(64) NOT NULL REFERENCES users(id),
    grant_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NULL,
    revocation_timestamp TIMESTAMP WITH TIME ZONE NULL,
    revoked_by VARCHAR(64) REFERENCES users(id)
);

ALTER TABLE document_grants ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE NULL;

CREATE INDEX IF NOT EXISTS idx_grants_doc_recip ON document_grants(document_id, recipient_id);
CREATE INDEX IF NOT EXISTS idx_grants_recipient ON document_grants(recipient_id);
CREATE INDEX IF NOT EXISTS idx_grants_revocation ON document_grants(revocation_timestamp);
CREATE INDEX IF NOT EXISTS idx_grants_expires ON document_grants(expires_at);

-- Enforce at most one active grant per (document_id, recipient_id) pair
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_grant 
ON document_grants (document_id, recipient_id) 
WHERE revocation_timestamp IS NULL;

-- 4. AUDIT EVENTS TABLE (Tamper-Evident Hash Chain)
CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(64) PRIMARY KEY,
    actor_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) REFERENCES documents(id) ON DELETE SET NULL,
    target_user_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    outcome VARCHAR(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILURE')),
    server_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_id VARCHAR(64) NOT NULL,
    sanitized_details TEXT,
    previous_hash VARCHAR(64) NOT NULL,
    current_hash VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_events(server_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_document ON audit_events(document_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_events(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_events(action);
CREATE INDEX IF NOT EXISTS idx_audit_outcome ON audit_events(outcome);
