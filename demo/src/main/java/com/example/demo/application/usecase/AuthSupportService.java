// application/service/AuthSupportService.java
package com.example.demo.application.usecase;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.adapter.persistence.audit_log.AuditLogService;
import com.example.demo.adapter.persistence.auth.auth_otp.AuthOtpEntity;
import com.example.demo.adapter.persistence.auth.auth_otp.AuthOtpRepository;
import com.example.demo.adapter.persistence.auth.login_attempt.LoginAttemptEntity;
import com.example.demo.adapter.persistence.auth.login_attempt.LoginAttemptRepository;
import com.example.demo.adapter.persistence.user.UserEntity;
import com.example.demo.adapter.persistence.user.UserJpaRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthSupportService {
  private final UserJpaRepository users;
  private final AuthOtpRepository otps;
  private final LoginAttemptRepository attempts;
  private final AuditLogService audit;
  private final BCryptPasswordEncoder encoder;
  private final EmailService emailService;
  private final long otpExpMin;
  private final int maxFailed;
  private final int windowMin;
  private final int blockMin;

  public AuthSupportService(
      UserJpaRepository users, AuthOtpRepository otps, LoginAttemptRepository attempts,
      AuditLogService audit, BCryptPasswordEncoder encoder, EmailService emailService,
      @Value("${app.mfa.otp-exp-minutes}") long otpExpMin,
      @Value("${app.auth.max-failed}") int maxFailed,
      @Value("${app.auth.window-minutes}") int windowMin,
      @Value("${app.auth.block-minutes}") int blockMin
  ) {
    this.users=users; this.otps=otps; this.attempts=attempts; this.audit=audit;
    this.encoder=encoder; this.emailService=emailService;
    this.otpExpMin=otpExpMin; this.maxFailed=maxFailed; this.windowMin=windowMin; this.blockMin=blockMin;
  }

  public Optional<UserEntity> findByIdentifier(String identifier) {
    // identifier could be email or username (both citext)
    var byEmail = users.findByEmail(identifier);
    if (byEmail.isPresent()) return byEmail;
    return users.findByUsername(identifier);
  }

  public boolean matchPassword(UserEntity u, String raw) { return encoder.matches(raw, u.getPasswordHash()); }

  public void recordAttempt(UserEntity u, String identifier, boolean success, String reason, HttpServletRequest req) {
    var a = new LoginAttemptEntity();
    a.setUserId(u != null ? u.getId() : null);
    a.setIdentifier(identifier);
    a.setSuccess(success);
    a.setReason(reason);
    a.setIp(req != null ? req.getRemoteAddr() : null);
    a.setUserAgent(req != null ? req.getHeader("User-Agent") : null);
    a.setCreatedAt(OffsetDateTime.now());
    attempts.save(a);
  }

  public boolean isBlocked(UserEntity u) {
    return u.getBlockedUntil() != null && u.getBlockedUntil().isAfter(OffsetDateTime.now());
  }

  public void maybeBlock(UserEntity u) {
    var since = OffsetDateTime.now().minusMinutes(windowMin);
    long failed = attempts.countFailedSince(u.getId(), since);
    if (failed >= maxFailed) {
      u.setBlockedUntil(OffsetDateTime.now().plusMinutes(blockMin));
      users.save(u);
    }
  }

  public AuthOtpRepository getOtpRepo() { return otps; }

  public UUID createAndSendOtp(UserEntity u) {
    var otp = new AuthOtpEntity();
    otp.setId(UUID.randomUUID());
    otp.setUserId(u.getId());
    String code = String.format("%06d", new Random().nextInt(1_000_000));
    otp.setCode(code);
    otp.setCreatedAt(OffsetDateTime.now());
    otp.setExpiresAt(OffsetDateTime.now().plusMinutes(otpExpMin));
    otps.save(otp);

    // Send email via EmailService
    try {
      emailService.sendOtpEmail(u.getEmail(), code);
      System.out.println("[MFA][EMAIL] Successfully sent OTP to " + u.getEmail());
    } catch (Exception e) {
      System.err.println("[MFA][EMAIL] Failed to send OTP to " + u.getEmail() + ": " + e.getMessage());
      // Still log to console as fallback for development
      System.out.println("[MFA][EMAIL] FALLBACK code=" + code);
    }

    // audit
    audit.logEvent("MFA_OTP_ISSUED", "user", u.getId(), null, Map.of("otpId", otp.getId().toString()), null);
    return otp.getId();
  }
}
