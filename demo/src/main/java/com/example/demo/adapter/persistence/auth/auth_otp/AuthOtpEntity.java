// adapter/persistence/auth/AuthOtpEntity.java
package com.example.demo.adapter.persistence.auth.auth_otp;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name="auth_otps", schema="public")
public class AuthOtpEntity {
  @Id @Column(columnDefinition = "uuid") 
  private UUID id;

  @Column(name="user_id", columnDefinition="uuid", nullable=false) 
  private UUID userId;

  @Column(nullable=false) 
  private String code;

  @Column(name="created_at", nullable=false) 
  private OffsetDateTime createdAt;
  
  @Column(name="expires_at", nullable=false) 
  private OffsetDateTime expiresAt;

  @Column(name="consumed_at") 
  private OffsetDateTime consumedAt;

  // Getters and Setters
  public UUID getId() { 
    return id; 
  }
  public void setId(UUID id) { 
    this.id = id; 
  }
  public UUID getUserId() { 
    return userId;
  }
  public void setUserId(UUID userId) {
    this.userId = userId;
  }
  public String getCode() {
    return code;
  }
  public void setCode(String code) {
    this.code = code;
  }
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }
  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }
  public OffsetDateTime getConsumedAt() {
    return consumedAt;
  }
  public void setConsumedAt(OffsetDateTime consumedAt) {
    this.consumedAt = consumedAt;
  }
}