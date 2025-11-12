package com.example.demo.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.example.demo.domain.model.User;

public interface UserRepositoryPort {
  User save(User user, String hashedPassword);
  Optional<User> findById(java.util.UUID id);
  List<User> find(String emailLike, String usernameLike, int limit, int offset);
  long count(String emailLike, String usernameLike);
  Optional<User> update(User user, Optional<String> newRawPassword);
  boolean delete(java.util.UUID id);
  boolean existsByEmail(String email);
  boolean existsByUsername(String username);
}
