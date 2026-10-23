package com.trustvault.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("application", "TrustVault");
        status.put("version", "1.0.0");
        status.put("timestamp", Instant.now().toString());

        try {
            Integer dbCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (dbCheck != null && dbCheck == 1) {
                status.put("database", "UP");
                status.put("status", "UP");
                return ResponseEntity.ok(status);
            } else {
                status.put("database", "DEGRADED");
                status.put("status", "DEGRADED");
                return ResponseEntity.status(503).body(status);
            }
        } catch (Exception e) {
            status.put("database", "DOWN");
            status.put("database_error", e.getMessage());
            status.put("status", "DOWN");
            return ResponseEntity.status(503).body(status);
        }
    }
}
