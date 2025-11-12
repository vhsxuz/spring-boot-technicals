// adapter/persistence/auth/LoginAttemptRepository.java
package com.example.demo.adapter.persistence.auth.login_attempt;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, Long> {
  @Query("""
    select count(a) from LoginAttemptEntity a
    where a.userId = :userId and a.success = false and a.createdAt >= :since
  """)
  long countFailedSince(@Param("userId") UUID userId, @Param("since") OffsetDateTime since);
}
