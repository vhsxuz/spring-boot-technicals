package com.example.demo.adapter.persistence.audit_log;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogService {
  private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

  private final AuditLogJpaRepository repository;
  private final ObjectMapper mapper;

  public AuditLogService(AuditLogJpaRepository repository, ObjectMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public void logEvent( String action, String entityType, Object entityId, UUID actorUserId, Object details, HttpServletRequest request) {
    try {
      AuditLogEntity row = new AuditLogEntity();
      row.setAction(action);
      row.setEntityType(entityType);
      row.setEntityId(entityId != null ? entityId.toString() : null);
      row.setActorUserId(actorUserId);
      if (request != null) {
        row.setMethod(request.getMethod());
        row.setPath(request.getRequestURI());
        row.setIpAddress(extractIp(request));
        row.setUserAgent(request.getHeader("User-Agent"));
      }
      row.setDetails(details != null ? mapper.writeValueAsString(details) : null);
      row.setCreatedAt(OffsetDateTime.now()); // optional; DB default also works

      repository.save(row);
    } catch (JsonProcessingException e) {
      log.warn("Failed to write audit log: action={}, entityType={}, entityId={}",
          action, entityType, entityId, e);
    }
  }

  private String extractIp(HttpServletRequest req) {
    // Respect common proxy headers if present
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    String realIp = req.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) return realIp;
    return req.getRemoteAddr();
  }

  public void created(String entity, Object id, UUID actor, HttpServletRequest req, Object details) {
    logEvent("CREATE", entity, id, actor, details, req);
  }
  public void read(String entity, Object id, UUID actor, HttpServletRequest req, Object details) {
    logEvent("READ", entity, id, actor, details, req);
  }
  public void updated(String entity, Object id, UUID actor, HttpServletRequest req, Object details) {
    logEvent("UPDATE", entity, id, actor, details, req);
  }
  public void deleted(String entity, Object id, UUID actor, HttpServletRequest req, Object details) {
    logEvent("DELETE", entity, id, actor, details, req);
  }
}
