// application/usecase/UserService.java
package com.example.demo.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.model.User;
import com.example.demo.domain.port.in.UserUseCase;
import com.example.demo.domain.port.out.UserRepositoryPort;

@Service
public class UserService implements UserUseCase {

  private final UserRepositoryPort repo;

  public UserService(UserRepositoryPort repo) { this.repo = repo; }

  @Override @Transactional
  public User create(User user, String rawPassword) {
    if (user.email() != null && repo.existsByEmail(user.email())) {
      throw new IllegalArgumentException("Email already registered");
    }
    if (user.username() != null && repo.existsByUsername(user.username())) {
      throw new IllegalArgumentException("Username already taken");
    }
    return repo.save(user, rawPassword);
  }

  @Override @Transactional(readOnly = true)
  public Optional<User> getById(UUID id) { return repo.findById(id); }

  @Override @Transactional(readOnly = true)
  public List<User> list(String emailLike, String usernameLike, int limit, int offset) {
    return repo.find(emailLike, usernameLike, limit, offset);
  }

  @Override @Transactional
  public Optional<User> update(User user, Optional<String> newRawPassword) {
    return repo.update(user, newRawPassword);
  }

  @Override @Transactional
  public boolean delete(UUID id) { return repo.delete(id); }

  @Override @Transactional(readOnly = true)
  public long count(String emailLike, String usernameLike) {
    return repo.count(emailLike, usernameLike);
  }
}