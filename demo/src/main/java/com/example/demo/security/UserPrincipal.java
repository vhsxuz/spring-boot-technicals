// security/UserPrincipal.java
package com.example.demo.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(
    UUID id, 
    String usernameOrEmail, 
    String passwordHash, 
    Set<String> roles
) implements UserDetails {
  @Override public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream().map(r -> (GrantedAuthority) () -> "ROLE_" + r).toList();
  }
  @Override public String getPassword() { 
    return passwordHash; 
  }
  @Override public String getUsername() { 
    return usernameOrEmail; 
  }
  @Override public boolean isAccountNonExpired() { 
    return true; 
  }
  @Override public boolean isAccountNonLocked() { 
    return true; 
  }
  @Override public boolean isCredentialsNonExpired() { 
    return true; 
  }
  @Override public boolean isEnabled() { 
    return true; 
  }
}