package com.trustvault.repository;

import com.trustvault.dto.GrantResponse;
import com.trustvault.model.DocumentGrant;
import com.trustvault.model.PermissionLevel;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class DocumentGrantRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentGrantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<DocumentGrant> grantRowMapper = new RowMapper<>() {
        @Override
        public DocumentGrant mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new DocumentGrant(
                rs.getString("id"),
                rs.getString("document_id"),
                rs.getString("recipient_id"),
                PermissionLevel.valueOf(rs.getString("permission_level")),
                rs.getString("granted_by"),
                rs.getTimestamp("grant_timestamp") != null ? rs.getTimestamp("grant_timestamp").toInstant() : null,
                rs.getTimestamp("revocation_timestamp") != null ? rs.getTimestamp("revocation_timestamp").toInstant() : null,
                rs.getString("revoked_by")
            );
        }
    };

    public Optional<DocumentGrant> findById(String id) {
        String sql = "SELECT id, document_id, recipient_id, permission_level, granted_by, grant_timestamp, revocation_timestamp, revoked_by FROM document_grants WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, grantRowMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<DocumentGrant> findActiveGrant(String documentId, String recipientId) {
        String sql = "SELECT id, document_id, recipient_id, permission_level, granted_by, grant_timestamp, revocation_timestamp, revoked_by FROM document_grants WHERE document_id = ? AND recipient_id = ? AND revocation_timestamp IS NULL";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, grantRowMapper, documentId, recipientId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(DocumentGrant grant) {
        String sql = """
            INSERT INTO document_grants (
                id, document_id, recipient_id, permission_level, granted_by,
                grant_timestamp, revocation_timestamp, revoked_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            grant.getId(),
            grant.getDocumentId(),
            grant.getRecipientId(),
            grant.getPermissionLevel().name(),
            grant.getGrantedBy(),
            grant.getGrantTimestamp() != null ? Timestamp.from(grant.getGrantTimestamp()) : Timestamp.from(java.time.Instant.now()),
            grant.getRevocationTimestamp() != null ? Timestamp.from(grant.getRevocationTimestamp()) : null,
            grant.getRevokedBy()
        );
    }

    public boolean revokeGrant(String grantId, String revokedByUserId) {
        String sql = "UPDATE document_grants SET revocation_timestamp = CURRENT_TIMESTAMP, revoked_by = ? WHERE id = ? AND revocation_timestamp IS NULL";
        return jdbcTemplate.update(sql, revokedByUserId, grantId) > 0;
    }

    public List<GrantResponse> findGrantResponsesByDocumentId(String documentId) {
        String sql = """
            SELECT 
                dg.id, dg.document_id, dg.recipient_id, ur.email AS recipient_email,
                dg.permission_level, dg.granted_by, ug.email AS granted_by_email,
                dg.grant_timestamp, dg.revocation_timestamp, dg.revoked_by, ub.email AS revoked_by_email
            FROM document_grants dg
            JOIN users ur ON dg.recipient_id = ur.id
            JOIN users ug ON dg.granted_by = ug.id
            LEFT JOIN users ub ON dg.revoked_by = ub.id
            WHERE dg.document_id = ?
            ORDER BY dg.grant_timestamp DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new GrantResponse(
            rs.getString("id"),
            rs.getString("document_id"),
            rs.getString("recipient_id"),
            rs.getString("recipient_email"),
            PermissionLevel.valueOf(rs.getString("permission_level")),
            rs.getString("granted_by"),
            rs.getString("granted_by_email"),
            rs.getTimestamp("grant_timestamp") != null ? rs.getTimestamp("grant_timestamp").toInstant() : null,
            rs.getTimestamp("revocation_timestamp") != null ? rs.getTimestamp("revocation_timestamp").toInstant() : null,
            rs.getString("revoked_by"),
            rs.getString("revoked_by_email"),
            rs.getTimestamp("revocation_timestamp") == null
        ), documentId);
    }
}
