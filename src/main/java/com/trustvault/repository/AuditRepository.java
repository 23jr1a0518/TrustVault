package com.trustvault.repository;

import com.trustvault.dto.AuditEventResponse;
import com.trustvault.model.AuditEvent;
import com.trustvault.model.AuditOutcome;
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
public class AuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AuditEvent> auditRowMapper = new RowMapper<>() {
        @Override
        public AuditEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AuditEvent(
                rs.getString("id"),
                rs.getString("actor_id"),
                rs.getString("action"),
                rs.getString("document_id"),
                rs.getString("target_user_id"),
                AuditOutcome.valueOf(rs.getString("outcome")),
                rs.getTimestamp("server_timestamp") != null ? rs.getTimestamp("server_timestamp").toInstant() : null,
                rs.getString("request_id"),
                rs.getString("sanitized_details"),
                rs.getString("previous_hash"),
                rs.getString("current_hash")
            );
        }
    };

    public Optional<AuditEvent> findLatestEvent() {
        String sql = "SELECT id, actor_id, action, document_id, target_user_id, outcome, server_timestamp, request_id, sanitized_details, previous_hash, current_hash FROM audit_events ORDER BY server_timestamp DESC, id DESC LIMIT 1";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, auditRowMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<AuditEvent> findAllChronological() {
        String sql = "SELECT id, actor_id, action, document_id, target_user_id, outcome, server_timestamp, request_id, sanitized_details, previous_hash, current_hash FROM audit_events ORDER BY server_timestamp ASC, id ASC";
        return jdbcTemplate.query(sql, auditRowMapper);
    }

    public void save(AuditEvent event) {
        String sql = """
            INSERT INTO audit_events (
                id, actor_id, action, document_id, target_user_id, outcome,
                server_timestamp, request_id, sanitized_details, previous_hash, current_hash
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            event.getId(),
            event.getActorId(),
            event.getAction(),
            event.getDocumentId(),
            event.getTargetUserId(),
            event.getOutcome().name(),
            event.getServerTimestamp() != null ? Timestamp.from(event.getServerTimestamp()) : Timestamp.from(java.time.Instant.now()),
            event.getRequestId(),
            event.getSanitizedDetails(),
            event.getPreviousHash(),
            event.getCurrentHash()
        );
    }

    public List<AuditEventResponse> findAllPaginated(int limit, int offset) {
        String sql = """
            SELECT 
                ae.id, ae.actor_id, ua.email AS actor_email, ae.action,
                ae.document_id, d.display_filename AS document_name,
                ae.target_user_id, ut.email AS target_user_email,
                ae.outcome, ae.server_timestamp, ae.request_id,
                ae.sanitized_details, ae.previous_hash, ae.current_hash
            FROM audit_events ae
            LEFT JOIN users ua ON ae.actor_id = ua.id
            LEFT JOIN documents d ON ae.document_id = d.id
            LEFT JOIN users ut ON ae.target_user_id = ut.id
            ORDER BY ae.server_timestamp DESC, ae.id DESC
            LIMIT ? OFFSET ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AuditEventResponse(
            rs.getString("id"),
            rs.getString("actor_id"),
            rs.getString("actor_email"),
            rs.getString("action"),
            rs.getString("document_id"),
            rs.getString("document_name"),
            rs.getString("target_user_id"),
            rs.getString("target_user_email"),
            AuditOutcome.valueOf(rs.getString("outcome")),
            rs.getTimestamp("server_timestamp") != null ? rs.getTimestamp("server_timestamp").toInstant() : null,
            rs.getString("request_id"),
            rs.getString("sanitized_details"),
            rs.getString("previous_hash"),
            rs.getString("current_hash")
        ), limit, offset);
    }

    public List<AuditEventResponse> findByDocumentId(String documentId, int limit, int offset) {
        String sql = """
            SELECT 
                ae.id, ae.actor_id, ua.email AS actor_email, ae.action,
                ae.document_id, d.display_filename AS document_name,
                ae.target_user_id, ut.email AS target_user_email,
                ae.outcome, ae.server_timestamp, ae.request_id,
                ae.sanitized_details, ae.previous_hash, ae.current_hash
            FROM audit_events ae
            LEFT JOIN users ua ON ae.actor_id = ua.id
            LEFT JOIN documents d ON ae.document_id = d.id
            LEFT JOIN users ut ON ae.target_user_id = ut.id
            WHERE ae.document_id = ?
            ORDER BY ae.server_timestamp DESC, ae.id DESC
            LIMIT ? OFFSET ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AuditEventResponse(
            rs.getString("id"),
            rs.getString("actor_id"),
            rs.getString("actor_email"),
            rs.getString("action"),
            rs.getString("document_id"),
            rs.getString("document_name"),
            rs.getString("target_user_id"),
            rs.getString("target_user_email"),
            AuditOutcome.valueOf(rs.getString("outcome")),
            rs.getTimestamp("server_timestamp") != null ? rs.getTimestamp("server_timestamp").toInstant() : null,
            rs.getString("request_id"),
            rs.getString("sanitized_details"),
            rs.getString("previous_hash"),
            rs.getString("current_hash")
        ), documentId, limit, offset);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_events", Long.class);
        return count != null ? count : 0L;
    }
}
