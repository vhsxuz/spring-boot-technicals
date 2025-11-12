// adapter/persistence/role/UserRoleEntity.java
package com.example.demo.adapter.user_role;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles", schema = "public")
public class UserRoleEntity {

  @EmbeddedId
  private UserRoleId id;

  @Column(name = "granted_at", nullable = false)
  private OffsetDateTime grantedAt = OffsetDateTime.now();

  public UserRoleEntity() {}
  public UserRoleEntity(UserRoleId id) { this.id = id; }

  public UserRoleId getId() { return id; }
  public void setId(UserRoleId id) { this.id = id; }

  public OffsetDateTime getGrantedAt() { return grantedAt; }
  public void setGrantedAt(OffsetDateTime grantedAt) { this.grantedAt = grantedAt; }
}
