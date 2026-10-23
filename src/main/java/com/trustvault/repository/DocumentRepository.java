package com.trustvault.repository;

import com.trustvault.dto.DocumentResponse;
import com.trustvault.model.Document;
import com.trustvault.model.PermissionLevel;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Document> documentRowMapper = new RowMapper<>() {
        @Override
        public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Document(
                rs.getString("id"),
                rs.getString("owner_id"),
                rs.getString("display_filename"),
                rs.getString("storage_object_key"),
                rs.getString("mime_type"),
                rs.getLong("file_size"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("checksum_sha256"),
                rs.getBoolean("is_deleted"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
            );
        }
    };

    public Optional<Document> findById(String id) {
        String sql = "SELECT id, owner_id, display_filename, storage_object_key, mime_type, file_size, category, description, checksum_sha256, is_deleted, created_at, updated_at FROM documents WHERE id = ? AND is_deleted = FALSE";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, documentRowMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Document> findAnyById(String id) {
        String sql = "SELECT id, owner_id, display_filename, storage_object_key, mime_type, file_size, category, description, checksum_sha256, is_deleted, created_at, updated_at FROM documents WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, documentRowMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(Document doc) {
        if (findAnyById(doc.getId()).isPresent()) {
            String updateSql = """
                UPDATE documents SET
                    display_filename = ?,
                    category = ?,
                    description = ?,
                    is_deleted = ?,
                    updated_at = ?
                WHERE id = ?
                """;
            jdbcTemplate.update(updateSql,
                doc.getDisplayFilename(),
                doc.getCategory(),
                doc.getDescription(),
                doc.isDeleted(),
                doc.getUpdatedAt() != null ? Timestamp.from(doc.getUpdatedAt()) : Timestamp.from(java.time.Instant.now()),
                doc.getId()
            );
        } else {
            String insertSql = """
                INSERT INTO documents (
                    id, owner_id, display_filename, storage_object_key, mime_type,
                    file_size, category, description, checksum_sha256, is_deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            jdbcTemplate.update(insertSql,
                doc.getId(),
                doc.getOwnerId(),
                doc.getDisplayFilename(),
                doc.getStorageObjectKey(),
                doc.getMimeType(),
                doc.getFileSize(),
                doc.getCategory(),
                doc.getDescription(),
                doc.getChecksumSha256(),
                doc.isDeleted(),
                doc.getCreatedAt() != null ? Timestamp.from(doc.getCreatedAt()) : Timestamp.from(java.time.Instant.now()),
                doc.getUpdatedAt() != null ? Timestamp.from(doc.getUpdatedAt()) : Timestamp.from(java.time.Instant.now())
            );
        }
    }

    public boolean updateMetadata(String id, String displayFilename, String category, String description) {
        String sql = "UPDATE documents SET display_filename = COALESCE(?, display_filename), category = COALESCE(?, category), description = COALESCE(?, description), updated_at = CURRENT_TIMESTAMP WHERE id = ? AND is_deleted = FALSE";
        return jdbcTemplate.update(sql, displayFilename, category, description, id) > 0;
    }

    public boolean softDelete(String id) {
        String sql = "UPDATE documents SET is_deleted = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND is_deleted = FALSE";
        return jdbcTemplate.update(sql, id) > 0;
    }

    public List<DocumentResponse> searchAccessibleDocuments(String currentUserId, String nameFilter, String ownerFilter, String categoryFilter, LocalDate dateFilter, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                d.id, d.owner_id, u.email as owner_email, d.display_filename, d.mime_type,
                d.file_size, d.category, d.description, d.checksum_sha256, d.created_at, d.updated_at,
                (d.owner_id = ?) as is_owner,
                dg.permission_level as grant_permission
            FROM documents d
            JOIN users u ON d.owner_id = u.id
            LEFT JOIN document_grants dg ON dg.document_id = d.id AND dg.recipient_id = ? AND dg.revocation_timestamp IS NULL
            WHERE d.is_deleted = FALSE
              AND (d.owner_id = ? OR dg.id IS NOT NULL)
        """);

        List<Object> params = new ArrayList<>();
        params.add(currentUserId);
        params.add(currentUserId);
        params.add(currentUserId);

        if (nameFilter != null && !nameFilter.isBlank()) {
            sql.append(" AND LOWER(d.display_filename) LIKE LOWER(?)");
            params.add("%" + nameFilter.trim() + "%");
        }

        if (ownerFilter != null && !ownerFilter.isBlank()) {
            sql.append(" AND LOWER(u.email) LIKE LOWER(?)");
            params.add("%" + ownerFilter.trim() + "%");
        }

        if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND LOWER(d.category) = LOWER(?)");
            params.add(categoryFilter.trim());
        }

        if (dateFilter != null) {
            sql.append(" AND CAST(d.created_at AS DATE) = ?");
            params.add(java.sql.Date.valueOf(dateFilter));
        }

        sql.append(" ORDER BY d.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 50);
        params.add(offset >= 0 ? offset : 0);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            boolean isOwner = rs.getBoolean("is_owner");
            String grantPermStr = rs.getString("grant_permission");
            PermissionLevel perm = null;
            if (grantPermStr != null) {
                perm = PermissionLevel.valueOf(grantPermStr);
            }

            return new DocumentResponse(
                rs.getString("id"),
                rs.getString("owner_id"),
                rs.getString("owner_email"),
                rs.getString("display_filename"),
                rs.getString("mime_type"),
                rs.getLong("file_size"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("checksum_sha256"),
                isOwner,
                perm,
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
            );
        }, params.toArray());
    }
}
