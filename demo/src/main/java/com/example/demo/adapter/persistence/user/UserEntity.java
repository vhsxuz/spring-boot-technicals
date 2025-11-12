package com.example.demo.adapter.persistence.user;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", schema = "public",
       indexes = {
         @Index(name = "idx_users_email_lower", columnList = "email"),
         @Index(name = "idx_users_username_lower", columnList = "username")
       })
public class  UserEntity {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "fullname", nullable = false, length = 300)
  private String fullname;

  @Column(name = "username", nullable = false, length = 100)
  private String username;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "is_email_verified", nullable = false)
  private Boolean isEmailVerified;

  @Column(name = "blocked_until")
  private java.time.OffsetDateTime blockedUntil;

  @Column(name = "created_at", nullable = false)
  private java.time.OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private java.time.OffsetDateTime updatedAt;

  public UUID getId() {
    return id;
  }
  public void setId(UUID id) {
    this.id = id;
  }
  public String getFullname() {
    return fullname;
  }
  public void setFullname(String fullname) {
    this.fullname = fullname;
  } 
  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }
  public String getPasswordHash() {
    return passwordHash;
  }
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }
  public Boolean getIsEmailVerified() {
    return isEmailVerified;
  }
  public void setIsEmailVerified(Boolean isEmailVerified) {
    this.isEmailVerified = isEmailVerified;
  }
  public java.time.OffsetDateTime getBlockedUntil() {
    return blockedUntil;
  }
  public void setBlockedUntil(java.time.OffsetDateTime blockedUntil) {
    this.blockedUntil = blockedUntil;
  }
  public java.time.OffsetDateTime getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(java.time.OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
  public java.time.OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
  public void setUpdatedAt(java.time.OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}