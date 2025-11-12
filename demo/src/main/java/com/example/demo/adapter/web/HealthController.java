package com.example.demo.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.domain.model.ApiResponse;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Service health and status endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check", description = "Check if the service is running. No authentication required.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthData = Map.of(
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString(),
                "service", "Spring Boot Technical Demo API",
                "version", "1.0.0"
        );

        return ResponseEntity.ok(ApiResponse.ok("Service is healthy", healthData));
    }
}
