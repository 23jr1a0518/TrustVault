package com.trustvault.repository;

import com.trustvault.model.AccountStatus;
import com.trustvault.model.Role;
import com.trustvault.model.User;
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
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = new RowMapper<>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(
                rs.getString("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                AccountStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
            );
        }
    };

    public Optional<User> findById(String id) {
        String sql = "SELECT id, email, password_hash, role, status, created_at, updated_at FROM users WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, userRowMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, role, status, created_at, updated_at FROM users WHERE LOWER(email) = LOWER(?)";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, userRowMapper, email));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(User user) {
        if (findById(user.getId()).isPresent()) {
            String updateSql = """
                UPDATE users SET
                    email = ?,
                    password_hash = ?,
                    role = ?,
                    status = ?,
                    updated_at = ?
                WHERE id = ?
                """;
            jdbcTemplate.update(updateSql,
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getUpdatedAt() != null ? Timestamp.from(user.getUpdatedAt()) : Timestamp.from(java.time.Instant.now()),
                user.getId()
            );
        } else {
            String insertSql = """
                INSERT INTO users (id, email, password_hash, role, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            jdbcTemplate.update(insertSql,
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt() != null ? Timestamp.from(user.getCreatedAt()) : Timestamp.from(java.time.Instant.now()),
                user.getUpdatedAt() != null ? Timestamp.from(user.getUpdatedAt()) : Timestamp.from(java.time.Instant.now())
            );
        }
    }

    public boolean updateStatus(String id, AccountStatus status) {
        String sql = "UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        return jdbcTemplate.update(sql, status.name(), id) > 0;
    }

    public List<User> findAll() {
        String sql = "SELECT id, email, password_hash, role, status, created_at, updated_at FROM users ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count != null ? count : 0L;
    }
}
