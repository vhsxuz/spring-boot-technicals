package com.example.demo.adapter.persistence.audit_log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    /**
     * Find audit logs with optional filters
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "(:actorUserId IS NULL OR a.actorUserId = :actorUserId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> findWithFilters(
        @Param("actorUserId") UUID actorUserId,
        @Param("action") String action,
        @Param("entityType") String entityType,
        @Param("fromDate") OffsetDateTime fromDate,
        @Param("toDate") OffsetDateTime toDate,
        Pageable pageable
    );
}
