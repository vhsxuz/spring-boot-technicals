package com.example.demo.adapter.persistence.auth.login_attempt;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name="login_attempts", schema="public")
public class LoginAttemptEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) 
  private Long id;

  @Column(name="user_id", columnDefinition="uuid") 
  private UUID userId;

  @Column(nullable=false) 
  private String identifier;
  
  @Column(nullable=false) 
  private Boolean success;

  private String reason;
  
  @Column(name="ip_address") 
  private String ip;

  @Column(name="user_agent") 
  private String userAgent;

  @Column(name="created_at", nullable=false) 
  private OffsetDateTime createdAt;
  
  // getters/setters...
  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }
  public UUID getUserId() {
    return userId;
  }
  public void setUserId(UUID userId) {
    this.userId = userId;
  }
  public String getIdentifier() {
    return identifier;
  }
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
  public Boolean getSuccess() {
    return success;
  }
  public void setSuccess(Boolean success) {
    this.success = success;
  }
  public String getReason() {
    return reason;
  }
  public void setReason(String reason) {
    this.reason = reason;
  }
  public String getIp() {
    return ip;
  }
  public void setIp(String ip) {
    this.ip = ip;
  }
  public String getUserAgent() {
    return userAgent;
  }
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

}