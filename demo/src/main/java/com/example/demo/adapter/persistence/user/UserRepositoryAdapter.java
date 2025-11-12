// adapter/persistence/user/UserRepositoryAdapter.java
package com.example.demo.adapter.persistence.user;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional; 
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.adapter.persistence.audit_log.AuditLogService;
import com.example.demo.adapter.persistence.role.RoleJpaRepository;
import com.example.demo.adapter.user_role.UserRoleEntity;
import com.example.demo.adapter.user_role.UserRoleId;
import com.example.demo.adapter.user_role.UserRoleJpaRepository;
import com.example.demo.domain.model.User;
import com.example.demo.domain.port.out.UserRepositoryPort;
import com.example.demo.domain.utils.SortUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {
  private static final Logger log = LoggerFactory.getLogger(UserRepositoryAdapter.class);

  private final UserJpaRepository jpa;
  private final BCryptPasswordEncoder encoder;
  private final AuditLogService audit;
  private final RoleJpaRepository roleRepo;
  private final UserRoleJpaRepository userRoleRepo;

  public UserRepositoryAdapter(UserJpaRepository jpa, AuditLogService audit, RoleJpaRepository roleRepo, UserRoleJpaRepository userRoleRepo) {
    this.jpa = jpa;
    this.audit = audit;
    this.encoder = new BCryptPasswordEncoder();
    this.roleRepo = roleRepo;
    this.userRoleRepo = userRoleRepo;
  }

  private HttpServletRequest currentRequest() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes sra) return sra.getRequest();
    return null;
  }

  @Override
  @Transactional
  public User save(User user, String rawPassword) {
    var now = OffsetDateTime.now();
    var e = new UserEntity();
    e.setId(user.id());
    e.setFullname(user.fullname());
    e.setUsername(user.username());
    e.setEmail(user.email());
    e.setPasswordHash(encoder.encode(rawPassword));
    e.setIsEmailVerified(Boolean.TRUE.equals(user.isEmailVerified()));
    e.setBlockedUntil(user.blockedUntil());
    e.setCreatedAt(now);
    e.setUpdatedAt(now);

    var saved = jpa.save(e);
    
    // fetch the VIEWER role id dynamically (no hard-coded UUID)
    var viewerRoleId = roleRepo.findIdByRoleKey("VIEWER")
        .orElseThrow(() -> new IllegalStateException("Default role 'VIEWER' not found"));
    userRoleRepo.save(new UserRoleEntity(new UserRoleId(saved.getId(), viewerRoleId)));

    log.info("[USER][CREATE] id={}, username={}, email={}", saved.getId(), saved.getUsername(), saved.getEmail());
    try {
      audit.logEvent(
          "USER_CREATE",
          "user",
          saved.getId(),
          null, // actor_user_id (fill from auth later if available)
          Map.of(
              "username", saved.getUsername(),
              "email", saved.getEmail(),
              "emailVerified", saved.getIsEmailVerified()
          ),
          currentRequest()
      );
    } catch (Exception ex) {
      log.warn("Audit log failed on USER_CREATE id={}: {}", saved.getId(), ex.getMessage());
    }

    return UserMapper.toDomain(saved);
  }

  // ---------- READ (by id) ----------
  @Override
  public Optional<User> findById(UUID id) {
    var res = jpa.findById(id).map(UserMapper::toDomain);
    log.info("[USER][READ] id={} found={}", id, res.isPresent());

    try {
      audit.logEvent(
          "USER_READ",
          "user",
          id,
          null,
          Map.of("found", res.isPresent()),
          currentRequest()
      );
    } catch (Exception ex) {
      log.warn("Audit log failed on USER_READ id={}: {}", id, ex.getMessage());
    }

    return res;
  }

  // ---------- LIST (with filters) ----------
  @Override
  public List<User> find(String emailLike, String usernameLike, int limit, int offset) {
    final int size = (limit > 0) ? limit : 20;
    final int page = Math.max(0, offset) / size;

    var pageRes = jpa.findFiltered(emailLike, usernameLike, PageRequest.of(page, size));
    var out = pageRes.getContent().stream().map(UserMapper::toDomain).toList();

    log.info("[USER][LIST] emailLike={}, usernameLike={}, size={}, offset={}, returned={}",
        emailLike, usernameLike, size, offset, out.size());

    // sort with your bubble sort util by fullname (then username) case-insensitively
    Comparator<User> byNameThenUsername =
        Comparator.<User, String>comparing(u -> Optional.ofNullable(u.fullname()).orElse(""), String.CASE_INSENSITIVE_ORDER)
                  .thenComparing(u -> Optional.ofNullable(u.username()).orElse(""), String.CASE_INSENSITIVE_ORDER);
    var sorted = SortUtils.bubbleSort(out, byNameThenUsername);

    try {
      audit.logEvent(
          "USER_LIST",
          "user",
          null,
          null,
          Map.of(
              "emailLike", String.valueOf(emailLike),
              "usernameLike", String.valueOf(usernameLike),
              "limit", size,
              "offset", offset,
              "returned", sorted.size()
          ),
          currentRequest()
      );
    } catch (Exception ex) {
      log.warn("Audit log failed on USER_LIST: {}", ex.getMessage());
    }

    return sorted;
  }

  // ---------- COUNT (for pagination summaries) ----------
  @Override
  public long count(String emailLike, String usernameLike) {
    long total = jpa.findFiltered(emailLike, usernameLike, PageRequest.of(0, 1)).getTotalElements();
    try {
      audit.logEvent(
          "USER_COUNT",
          "user",
          null,
          null,
          Map.of(
              "emailLike", String.valueOf(emailLike),
              "usernameLike", String.valueOf(usernameLike),
              "total", total
          ),
          currentRequest()
      );
    } catch (Exception ex) {
      log.warn("Audit log failed on USER_COUNT: {}", ex.getMessage());
    }
    return total;
  }

  // ---------- UPDATE ----------
  @Override
  public Optional<User> update(User user, Optional<String> newRawPassword) {
    return jpa.findById(user.id()).map(existing -> {
      var before = Map.of(
          "fullname", existing.getFullname(),
          "username", existing.getUsername(),
          "email", existing.getEmail(),
          "emailVerified", existing.getIsEmailVerified(),
          "blockedUntil", String.valueOf(existing.getBlockedUntil())
      );

      if (user.fullname() != null) existing.setFullname(user.fullname());
      if (user.username() != null) existing.setUsername(user.username());
      if (user.email() != null) existing.setEmail(user.email());
      if (user.isEmailVerified() != null) existing.setIsEmailVerified(user.isEmailVerified());
      // allow clearing
      if (user.blockedUntil() != null || user.blockedUntil() == null) {
        existing.setBlockedUntil(user.blockedUntil());
      }
      newRawPassword.ifPresent(pw -> existing.setPasswordHash(encoder.encode(pw)));
      existing.setUpdatedAt(OffsetDateTime.now());

      var saved = jpa.save(existing);
      log.info("[USER][UPDATE] id={}", saved.getId());

      var after = Map.of(
          "fullname", saved.getFullname(),
          "username", saved.getUsername(),
          "email", saved.getEmail(),
          "emailVerified", saved.getIsEmailVerified(),
          "blockedUntil", String.valueOf(saved.getBlockedUntil()),
          "passwordChanged", newRawPassword.isPresent()
      );

      try {
        audit.logEvent(
            "USER_UPDATE",
            "user",
            saved.getId(),
            null,
            Map.of("before", before, "after", after),
            currentRequest()
        );
      } catch (Exception ex) {
        log.warn("Audit log failed on USER_UPDATE id={}: {}", saved.getId(), ex.getMessage());
      }

      return UserMapper.toDomain(saved);
    });
  }

  // ---------- DELETE ----------
  @Override
  public boolean delete(UUID id) {
    if (!jpa.existsById(id)) {
      log.info("[USER][DELETE] id={} not-found", id);
      try {
        audit.logEvent(
            "USER_DELETE",
            "user",
            id,
            null,
            Map.of("deleted", false, "reason", "not-found"),
            currentRequest()
        );
      } catch (Exception ex) {
        log.warn("Audit log failed on USER_DELETE (not-found) id={}: {}", id, ex.getMessage());
      }
      return false;
    }

    userRoleRepo.deleteByUserId(id);
    jpa.deleteById(id);
    log.info("[USER][DELETE] id={} deleted", id);

    try {
      audit.logEvent(
          "USER_DELETE",
          "user",
          id,
          null,
          Map.of("deleted", true),
          currentRequest()
      );
    } catch (Exception ex) {
      log.warn("Audit log failed on USER_DELETE id={}: {}", id, ex.getMessage());
    }

    return true;
  }

  @Override public boolean existsByEmail(String email) {
    return email != null && jpa.existsByEmail(email);
  }

  @Override public boolean existsByUsername(String username) {
    return username != null && jpa.existsByUsername(username);
  }
}