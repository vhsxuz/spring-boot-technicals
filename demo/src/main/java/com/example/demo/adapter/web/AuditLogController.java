package com.example.demo.adapter.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.adapter.persistence.audit_log.AuditLogEntity;
import com.example.demo.adapter.persistence.audit_log.AuditLogJpaRepository;
import com.example.demo.domain.model.ApiResponse;
import com.example.demo.security.AuthorizationService;
import com.example.demo.security.UserPrincipal;

/**
 * Controller for viewing audit logs.
 * Only accessible to SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogJpaRepository auditLogRepository;
    private final AuthorizationService authz;

    public AuditLogController(AuditLogJpaRepository auditLogRepository, AuthorizationService authz) {
        this.auditLogRepository = auditLogRepository;
        this.authz = authz;
    }

    /**
     * List audit logs with optional filters
     * Only SUPER_ADMIN can access this endpoint
     *
     * @param actorUserId Filter by user who performed the action
     * @param action Filter by action type (e.g., "CREATE", "UPDATE", "DELETE")
     * @param entityType Filter by entity type (e.g., "User", "Article")
     * @param fromDate Filter by start date (ISO 8601 format)
     * @param toDate Filter by end date (ISO 8601 format)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of audit logs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogEntity>>> list(
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // Only SUPER_ADMIN can view audit logs
        try {
            authz.requireCanViewAuditLogs();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogEntity> logs = auditLogRepository.findWithFilters(
                actorUserId, action, entityType, fromDate, toDate, pageable
        );

        return ResponseEntity.ok(ApiResponse.ok("OK", logs));
    }

    /**
     * Get a single audit log by ID
     * Only SUPER_ADMIN can access this endpoint
     *
     * @param id Audit log ID
     * @return Audit log details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogEntity>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // Only SUPER_ADMIN can view audit logs
        try {
            authz.requireCanViewAuditLogs();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
        }

        var log = auditLogRepository.findById(id);
        if (log.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "Audit log not found"));
        }

        return ResponseEntity.ok(ApiResponse.ok("OK", log.get()));
    }
}
