// adapter/persistence/user/UserMapper.java
package com.example.demo.adapter.persistence.user;

import com.example.demo.domain.model.User;

public final class UserMapper {
  private UserMapper() {}

  public static User toDomain(UserEntity e) {
    return new User(
        e.getId(),
        e.getFullname(),
        e.getUsername(),
        e.getEmail(),
        e.getIsEmailVerified(),
        e.getBlockedUntil(),
        e.getCreatedAt(),
        e.getUpdatedAt()
    );
  }
}
