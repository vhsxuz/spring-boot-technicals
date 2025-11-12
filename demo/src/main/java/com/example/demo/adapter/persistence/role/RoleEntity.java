package com.example.demo.adapter.persistence.role;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name = "roles", schema = "public")
public class RoleEntity {
  @Id @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "role_key", nullable = false)
  private String roleKey;

  @Column(name = "name", nullable = false)
  private String name;

  // getters/setters
  public UUID getId() { 
    return id; 
  }
  public void setId(UUID id) { 
    this.id = id; 
  }
  public String getRoleKey() { 
    return roleKey; 
  }
  public void setRoleKey(String roleKey) { 
    this.roleKey = roleKey; 
  }
  public String getName() { 
    return name; 
  }
  public void setName(String name) { 
    this.name = name; 
  }
}
