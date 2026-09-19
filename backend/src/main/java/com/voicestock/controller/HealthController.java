package com.voicestock.controller;

import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.voicestock.dto.HealthResponse;
import com.voicestock.service.DatabaseHealthService;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final DatabaseHealthService databaseHealth;

    public HealthController(DatabaseHealthService databaseHealth) { this.databaseHealth = databaseHealth; }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        String database = databaseHealth.status();
        boolean available = !"DOWN".equals(database);
        return ResponseEntity.status(available ? 200 : 503)
                .cacheControl(CacheControl.noStore())
                .body(new HealthResponse(available ? "UP" : "DOWN", "voicestock-api", database, Instant.now()));
    }
}
