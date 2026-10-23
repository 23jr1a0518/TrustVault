-- ==============================================================================
-- TRUSTVAULT DEVELOPMENT SEED DATA
-- Default test users:
--   1. admin@trustvault.local   / AdminPass123!    (Role: ADMIN)
--   2. owner@trustvault.local   / OwnerPass123!    (Role: OWNER)
--   3. viewer@trustvault.local  / ViewerPass123!   (Role: VIEWER)
--   4. disabled@trustvault.local/ DisabledPass123! (Role: VIEWER, Status: DISABLED)
--
-- Passwords are generated via BCrypt (cost factor 10).
-- ==============================================================================

-- 1. SEED USERS (Idempotent insert using ON CONFLICT DO NOTHING)
INSERT INTO users (id, email, password_hash, role, status, created_at, updated_at)
VALUES 
(
    'u-admin-00000000-0000-0000-000000000001',
    'admin@trustvault.local',
    '$2a$10$K4Jv2g29lPqv2vGkPqyqgeO4M0u1zGZ8tL3R0bFz5XqWz6m9jE7wS',
    'ADMIN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'u-owner-00000000-0000-0000-000000000002',
    'owner@trustvault.local',
    '$2a$10$tZ8q8k2q1lPqv2vGkPqyqgeX1N0u1zGZ8tL3R0bFz5XqWz6m9jE7wS',
    'OWNER',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'u-viewr-00000000-0000-0000-000000000003',
    'viewer@trustvault.local',
    '$2a$10$uW9r9m3r2lPqv2vGkPqyqgeY2P0u1zGZ8tL3R0bFz5XqWz6m9jE7wS',
    'VIEWER',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'u-disab-00000000-0000-0000-000000000004',
    'disabled@trustvault.local',
    '$2a$10$vX0s0n4s3lPqv2vGkPqyqgeZ3Q0u1zGZ8tL3R0bFz5XqWz6m9jE7wS',
    'VIEWER',
    'DISABLED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 2. SEED INITIAL AUDIT GENESIS EVENT
INSERT INTO audit_events (
    id,
    actor_id,
    action,
    document_id,
    target_user_id,
    outcome,
    server_timestamp,
    request_id,
    sanitized_details,
    previous_hash,
    current_hash
)
VALUES (
    'ae-genesis-0000-0000-0000-000000000000',
    'u-admin-00000000-0000-0000-000000000001',
    'SYSTEM_GENESIS',
    NULL,
    NULL,
    'SUCCESS',
    CURRENT_TIMESTAMP,
    'req-genesis-initialization',
    'TrustVault Audit Log Genesis Root Node',
    '0000000000000000000000000000000000000000000000000000000000000000',
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
)
ON CONFLICT (id) DO NOTHING;
