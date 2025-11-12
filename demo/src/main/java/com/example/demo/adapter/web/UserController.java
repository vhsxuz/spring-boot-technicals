package com.example.demo.adapter.web;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.adapter.persistence.role.RoleEntity;
import com.example.demo.adapter.persistence.role.RoleJpaRepository;
import com.example.demo.adapter.user_role.UserRoleEntity;
import com.example.demo.adapter.user_role.UserRoleId;
import com.example.demo.adapter.user_role.UserRoleJpaRepository;
import com.example.demo.adapter.web.dto.UserView;
import com.example.demo.adapter.web.dto.UserView.RoleItem;
import com.example.demo.domain.model.ApiResponse;
import com.example.demo.domain.model.User;
import com.example.demo.domain.port.in.UserUseCase;
import com.example.demo.adapter.persistence.audit_log.AuditLogService;
import com.example.demo.security.AuthorizationService;
import com.example.demo.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

  private final UserUseCase useCase;
  private final AuditLogService audit;
  private final RoleJpaRepository roleRepo;
  private final UserRoleJpaRepository userRoleRepo;
  private final AuthorizationService authz;

  public UserController(UserUseCase useCase,
                        AuditLogService audit,
                        RoleJpaRepository roleRepo,
                        UserRoleJpaRepository userRoleRepo,
                        AuthorizationService authz) {
    this.useCase = useCase;
    this.audit = audit;
    this.roleRepo = roleRepo;
    this.userRoleRepo = userRoleRepo;
    this.authz = authz;
  }

  private static Map<String, Object> mapNonNull(Object[][] entries) {
    var m = new java.util.LinkedHashMap<String, Object>();
    for (var e : entries) if (e[1] != null) m.put((String) e[0], e[1]);
    return m;
  }

  // -------- CREATE (auto-assign VIEWER happens in adapter) --------
  // Only SUPER_ADMIN can create users via this endpoint
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<UserView>> create(
      @RequestBody CreateUserBody body,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    // Only SUPER_ADMIN can create users
    try {
      authz.requireCanManageUsers();
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
    }

    UUID id = UUID.randomUUID();
    var now = OffsetDateTime.now();
    var toCreate = new User(
        id, body.fullname(), body.username(), body.email(),
        false, null, now, now
    );
    var created = useCase.create(toCreate, body.password());

    // fetch roles for view
    var roles = mapRoles(roleRepo.findByUserId(created.id()));

    audit.created("User", created.id(), authz.getCurrentUserId(), req,
        mapNonNull(new Object[][] {{"username", created.username()}, {"email", created.email()}}));

    return ResponseEntity
        .created(URI.create("/users/" + created.id()))
        .body(ApiResponse.created("User created", new UserView(created, roles)));
  }

  // -------- READ one (display roles) --------
  // Users can view their own profile, SUPER_ADMIN can view anyone
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserView>> get(
      @PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    // Check if user can view this profile
    try {
      authz.requireCanUpdateUser(id);  // Same permission: own profile or admin
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
    }

    var found = useCase.getById(id);
    if (found.isEmpty()) {
      return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "User not found"));
    }
    var user = found.get();
    var roles = mapRoles(roleRepo.findByUserId(id));
    audit.read("User", id, authz.getCurrentUserId(), req, mapNonNull(new Object[][] {{"hasRoles", !roles.isEmpty()}}));
    return ResponseEntity.ok(ApiResponse.ok("OK", new UserView(user, roles)));
  }

  // -------- LIST (each item shows roles) --------
  // Only SUPER_ADMIN can list all users
  @GetMapping
  public ResponseEntity<ApiResponse<List<UserView>>> list(
      @RequestParam(required = false) String emailLike,
      @RequestParam(required = false) String usernameLike,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    // Only SUPER_ADMIN can list users
    try {
      authz.requireCanManageUsers();
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
    }

    var users = useCase.list(emailLike, usernameLike, limit, offset);

    // bulk map roles per user (simple N calls; optimize with batch if needed)
    var out = new ArrayList<UserView>(users.size());
    for (var u : users) {
      var roles = mapRoles(roleRepo.findByUserId(u.id()));
      out.add(new UserView(u, roles));
    }

    audit.logEvent("LIST", "User", null, authz.getCurrentUserId(),
        mapNonNull(new Object[][] {{"limit", limit}, {"offset", offset}, {"returned", out.size()}}), req);

    return ResponseEntity.ok(ApiResponse.ok("OK", out));
  }

  // -------- UPDATE (optionally replace role by roleId) --------
  // Users can update their own profile (except roles), SUPER_ADMIN can update anyone
  @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<ApiResponse<UserView>> update(
      @PathVariable UUID id,
      @RequestBody UpdateUserBody body,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    // Check if user can update this profile
    try {
      authz.requireCanUpdateUser(id);
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
    }

    // Check if role assignment is requested
    if (body.roleId() != null) {
      // Only SUPER_ADMIN can assign roles
      try {
        authz.requireCanAssignRoles();
      } catch (AccessDeniedException e) {
        return ResponseEntity.status(403).body(
            ApiResponse.error(403, "Forbidden", "Only SUPER_ADMIN can assign roles"));
      }
    }

    var patch = new User(
        id, body.fullname(), body.username(), body.email(),
        body.isEmailVerified(), body.blockedUntil(), null, null
    );
    var updated = useCase.update(patch, Optional.ofNullable(body.newPassword()));
    if (updated.isEmpty()) {
      return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "User not found"));
    }

    var u = updated.get();

    // UPDATE: if roleId provided, replace all roles with that id
    if (body.roleId() != null) {
      userRoleRepo.deleteByUserId(id);
      userRoleRepo.save(new UserRoleEntity(new UserRoleId(id, body.roleId())));
    }

    var roles = mapRoles(roleRepo.findByUserId(id));

    audit.updated("User", id, authz.getCurrentUserId(), req,
        mapNonNull(new Object[][] {
            {"fullname", body.fullname()},
            {"username", body.username()},
            {"email", body.email()},
            {"isEmailVerified", body.isEmailVerified()},
            {"blockedUntil", body.blockedUntil()},
            {"roleChanged", body.roleId() != null}
        }));

    return ResponseEntity.ok(ApiResponse.ok("Updated", new UserView(u, roles)));
  }

  // -------- DELETE (roles removed in adapter) --------
  // Only SUPER_ADMIN can delete users
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    // Only SUPER_ADMIN can delete users
    try {
      authz.requireCanManageUsers();
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
    }

    boolean ok = useCase.delete(id);
    if (ok) {
      audit.deleted("User", id, authz.getCurrentUserId(), req, mapNonNull(new Object[][] {{"id", id}}));
      return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }
    return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "User not found"));
  }

  // ---- DTOs ----
  public record CreateUserBody(String fullname, String username, String email, String password) {}

  public record UpdateUserBody(
      String fullname,
      String username,
      String email,
      Boolean isEmailVerified,
      java.time.OffsetDateTime blockedUntil,
      String newPassword,
      UUID roleId
  ) {}

  // ---- helpers ----
  private static List<RoleItem> mapRoles(List<RoleEntity> rows) {
    return rows.stream()
        .map(r -> new RoleItem(r.getId(), r.getRoleKey(), r.getName()))
        .toList();
  }
}