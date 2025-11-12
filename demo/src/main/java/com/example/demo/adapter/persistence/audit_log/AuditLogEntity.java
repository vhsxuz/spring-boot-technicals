package com.example.demo.adapter.persistence.audit_log;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs", schema = "public")
public class AuditLogEntity {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private java.util.UUID id;

  @Column(name = "actor_user_id", columnDefinition = "uuid")
  private java.util.UUID actorUserId;

  @Column(nullable = false)
  private String action;

  @Column(name = "entity_type", nullable = false)
  private String entityType;

  @Column(name = "entity_id")
  private String entityId;

  private String method;
  private String path;

  // IP address column - varchar for compatibility with both PostgreSQL and H2
  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "user_agent")
  private String userAgent;

  // ✅ jsonb column — tell Hibernate it's JSON and cast on write
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private String details;

  @Column(name = "created_at", nullable = false)
  private java.time.OffsetDateTime createdAt;

  // getters/setters/constructors

  public UUID getId() { 
    return id; 
  }
  public void setId(UUID id) { 
    this.id = id; 
  }

  public UUID getActorUserId() { 
    return actorUserId;   
  }
  public void setActorUserId(UUID actorUserId) { 
    this.actorUserId = actorUserId; 
  }

  public String getAction() {
     return action; 
  }
  public void setAction(String action) { 
    this.action = action; 
  }

  public String getEntityType() { 
    return entityType; 
  }
  public void setEntityType(String entityType) { 
    this.entityType = entityType; 
  }

  public String getEntityId() { 
    return entityId; 
  }
  public void setEntityId(String entityId) { 
    this.entityId = entityId; 
  }

  public String getMethod() { 
    return method; 
  }
  public void setMethod(String method) { 
    this.method = method; 
  }

  public String getPath() { 
    return path; 
  }
  public void setPath(String path) { 
    this.path = path; 
  }

  public String getIpAddress() { 
    return ipAddress; 
  }
  public void setIpAddress(String ipAddress) { 
    this.ipAddress = ipAddress; 
  }

  public String getUserAgent() { 
    return userAgent; 
  }
  public void setUserAgent(String userAgent) { 
    this.userAgent = userAgent; 
  }

  public String getDetails() { 
    return details; 
  }
  public void setDetails(String details) { 
    this.details = details; 
  }

  public OffsetDateTime getCreatedAt() { 
    return createdAt; 
  }
  public void setCreatedAt(OffsetDateTime createdAt) { 
    this.createdAt = createdAt; 
  }
}
