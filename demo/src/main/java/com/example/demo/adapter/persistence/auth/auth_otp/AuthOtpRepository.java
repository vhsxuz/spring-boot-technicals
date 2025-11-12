package com.example.demo.adapter.persistence.auth.auth_otp;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthOtpRepository extends JpaRepository<AuthOtpEntity, UUID> {
  Optional<AuthOtpEntity> findByIdAndUserId(UUID id, UUID userId);
}