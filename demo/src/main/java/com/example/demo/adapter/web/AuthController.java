// adapter/web/AuthController.java
package com.example.demo.adapter.web;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.demo.adapter.persistence.audit_log.AuditLogService;
import com.example.demo.adapter.persistence.role.RoleJpaRepository;
import com.example.demo.adapter.persistence.user.UserEntity;
import com.example.demo.adapter.persistence.user.UserJpaRepository;
import com.example.demo.adapter.user_role.UserRoleEntity;
import com.example.demo.adapter.user_role.UserRoleId;
import com.example.demo.adapter.user_role.UserRoleJpaRepository;
import com.example.demo.application.usecase.AuthSupportService;
import com.example.demo.domain.model.ApiResponse;
import com.example.demo.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and authorization endpoints with MFA support")
public class AuthController {
  private final UserJpaRepository users;
  private final RoleJpaRepository roles;
  private final UserRoleJpaRepository userRoles;
  private final BCryptPasswordEncoder encoder;
  private final JwtService jwt;
  private final AuthSupportService auth;
  private final AuditLogService audit;

  public AuthController(
      UserJpaRepository users, RoleJpaRepository roles, UserRoleJpaRepository userRoles,
      BCryptPasswordEncoder encoder, JwtService jwt, AuthSupportService auth, AuditLogService audit
  ) {
    this.users=users; this.roles=roles; this.userRoles=userRoles;
    this.encoder=encoder; this.jwt=jwt; this.auth=auth; this.audit=audit;
  }

  // ---- Register (default role VIEWER; email OTP required to verify email) ----
  @Operation(summary = "Register new user", description = "Register a new user account with default VIEWER role. OTP will be sent to email for verification.")
  @PostMapping("/register") @Transactional
  public ResponseEntity<com.example.demo.domain.model.ApiResponse<Map<String,Object>>> register(@RequestBody RegisterBody b, HttpServletRequest req) {
    if (users.findByEmail(b.email()).isPresent()) {
      return ResponseEntity.badRequest().body(ApiResponse.error(400, "Email exists", "Email already registered"));
    }
    if (users.findByUsername(b.username()).isPresent()) {
      return ResponseEntity.badRequest().body(ApiResponse.error(400, "Username exists", "Username already taken"));
    }

    var u = new UserEntity();
    u.setId(UUID.randomUUID());
    u.setFullname(b.fullname());
    u.setUsername(b.username());
    u.setEmail(b.email());
    u.setPasswordHash(encoder.encode(b.password()));
    u.setIsEmailVerified(false);
    u.setCreatedAt(OffsetDateTime.now());
    u.setUpdatedAt(OffsetDateTime.now());
    users.save(u);

    // assign default role VIEWER (resolved by key, no hard-coded UUID)
    var viewerRoleId = roles.findIdByRoleKey("VIEWER").orElseThrow(() -> new IllegalStateException("VIEWER role missing"));
    userRoles.save(new UserRoleEntity(new UserRoleId(u.getId(), viewerRoleId)));

    var otpId = auth.createAndSendOtp(u);

    audit.logEvent("AUTH_REGISTER", "user", u.getId(), null,
        Map.of("username", b.username(), "email", b.email()), req);

    var payload = Map.<String,Object>of(
        "userId", u.getId().toString(),
        "otpId", otpId.toString(),
        "message", "OTP sent to email. Call /auth/verify-otp to complete."
    );
    return ResponseEntity.ok(ApiResponse.ok("Registered", payload));
  }

  // ---- Login (identifier=email or username; returns pending token + otpId) ----
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Map<String,Object>>> login(@RequestBody LoginBody b, HttpServletRequest req) {
    var uOpt = auth.findByIdentifier(b.identifier());
    if (uOpt.isEmpty()) {
      audit.logEvent("AUTH_LOGIN_FAILED", "user", null, null, Map.of("reason", "user_not_found"), req);
      return ResponseEntity.status(401).body(ApiResponse.error(401, "Unauthorized", "Invalid credentials"));
    }
    var u = uOpt.get();
    if (auth.isBlocked(u)) {
      auth.recordAttempt(u, b.identifier(), false, "blocked", req);
      audit.logEvent("AUTH_LOGIN_BLOCKED", "user", u.getId(), null, Map.of("blockedUntil", String.valueOf(u.getBlockedUntil())), req);
      return ResponseEntity.status(423).body(ApiResponse.error(423, "Locked", "Account temporarily blocked"));
    }
    if (!auth.matchPassword(u, b.password())) {
      auth.recordAttempt(u, b.identifier(), false, "bad_password", req);
      auth.maybeBlock(u);
      audit.logEvent("AUTH_LOGIN_FAILED", "user", u.getId(), null, Map.of("reason", "bad_password"), req);
      return ResponseEntity.status(401).body(ApiResponse.error(401, "Unauthorized", "Invalid credentials"));
    }

    // Password ok -> issue OTP & pending token
    var otpId = auth.createAndSendOtp(u);
    var pending = jwt.issueAccess(u.getId().toString(), Map.of("mfa", "pending"));
    auth.recordAttempt(u, b.identifier(), true, "password_ok_mfa_pending", req);
    audit.logEvent("AUTH_LOGIN_PASSWORD_OK", "user", u.getId(), null, Map.of("otpId", otpId.toString()), req);

    var payload = Map.<String,Object>of(
        "otpId", otpId.toString(),
        "pendingToken", pending,
        "message", "OTP sent. Verify with /auth/verify-otp."
    );
    return ResponseEntity.ok(ApiResponse.ok("MFA required", payload));
  }

  // ---- Verify OTP -> issue access token with roles ----
  @PostMapping("/verify-otp")
  public ResponseEntity<ApiResponse<Map<String,Object>>> verifyOtp(@RequestBody VerifyOtpBody b, HttpServletRequest req) {
    var otpRepo = auth.getOtpRepo();
    var otp = otpRepo.findByIdAndUserId(b.otpId(), b.userId()).orElse(null);
    if (otp == null || otp.getConsumedAt() != null || otp.getExpiresAt().isBefore(OffsetDateTime.now())) {
      audit.logEvent("MFA_OTP_FAILED", "user", b.userId(), null, Map.of("reason","invalid_or_expired"), req);
      return ResponseEntity.status(401).body(ApiResponse.error(401, "Unauthorized", "Invalid/expired OTP"));
    }
    if (!Objects.equals(otp.getCode(), b.code())) {
      audit.logEvent("MFA_OTP_FAILED", "user", b.userId(), null, Map.of("reason","wrong_code"), req);
      return ResponseEntity.status(401).body(ApiResponse.error(401, "Unauthorized", "Invalid/expired OTP"));
    }

    // Mark consumed; verify email on first success
    otp.setConsumedAt(OffsetDateTime.now());
    otpRepo.save(otp);

    var u = users.findById(b.userId()).orElseThrow();
    if (Boolean.FALSE.equals(u.getIsEmailVerified())) {
      u.setIsEmailVerified(true);
      users.save(u);
    }

    var roleKeys = userRoles.findRoleKeysByUserId(u.getId());
    var token = jwt.issueAccess(u.getId().toString(), Map.of("roles", roleKeys));

    audit.logEvent("AUTH_LOGIN_SUCCESS", "user", u.getId(), null, Map.of("roles", roleKeys), req);

    return ResponseEntity.ok(ApiResponse.ok("OK", Map.of(
        "accessToken", token,
        "tokenType", "Bearer",
        "user", Map.of("id", u.getId().toString(), "fullname", u.getFullname(),
                       "username", u.getUsername(), "email", u.getEmail(), "roles", roleKeys)
    )));
  }

  // ---- Me ----
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<Map<String,Object>>> me(
      @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.demo.security.UserPrincipal p,
      HttpServletRequest req
  ) {
    if (p == null) return ResponseEntity.status(401).body(ApiResponse.error(401, "Unauthorized", "No token"));
    var u = users.findById(p.id()).orElseThrow();
    var roles = userRoles.findRoleKeysByUserId(p.id());

    audit.logEvent("AUTH_ME", "user", u.getId(), p.id(), Map.of("roles", roles), req);

    return ResponseEntity.ok(ApiResponse.ok("OK", Map.of(
        "id", u.getId().toString(), "fullname", u.getFullname(), "username", u.getUsername(),
        "email", u.getEmail(), "roles", roles
    )));
  }

  // DTOs
  public record RegisterBody(
      @NotBlank String fullname,
      @NotBlank String username,
      @Email @NotBlank String email,
      @Size(min=8) String password
  ) {}
  public record LoginBody(@NotBlank String identifier, @NotBlank String password) {}
  public record VerifyOtpBody(@NotNull UUID otpId, @NotNull UUID userId, @NotBlank String code) {}
}
