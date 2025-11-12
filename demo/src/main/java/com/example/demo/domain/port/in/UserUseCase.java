package com.example.demo.domain.port.in;

import java.util.List;
import java.util.Optional;

import com.example.demo.domain.model.User;

public interface UserUseCase {
  User create(User user, String rawPassword);
  Optional<User> getById(java.util.UUID id);
  List<User> list(String emailLike, String usernameLike, int limit, int offset);
  Optional<User> update(User user, Optional<String> newRawPassword);
  boolean delete(java.util.UUID id);
  long count(String emailLike, String usernameLike);
}