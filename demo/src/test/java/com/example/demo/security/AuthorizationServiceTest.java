package com.example.demo.security;

import com.example.demo.domain.model.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AuthorizationService Unit Tests")
class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(UUID userId, String email, Set<String> roles) {
        UserPrincipal principal = new UserPrincipal(userId, email, "hashedPassword", roles);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ========== getCurrentUser Tests ==========

    @Test
    @DisplayName("getCurrentUser returns null when no authentication")
    void getCurrentUser_WhenNotAuthenticated_ReturnsNull() {
        assertThat(authorizationService.getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("getCurrentUser returns UserPrincipal when authenticated")
    void getCurrentUser_WhenAuthenticated_ReturnsUserPrincipal() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "test@example.com", Set.of("VIEWER"));

        UserPrincipal result = authorizationService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.usernameOrEmail()).isEqualTo("test@example.com");
    }

    // ========== getCurrentUserId Tests ==========

    @Test
    @DisplayName("getCurrentUserId throws when not authenticated")
    void getCurrentUserId_WhenNotAuthenticated_ThrowsException() {
        assertThatThrownBy(() -> authorizationService.getCurrentUserId())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authenticated");
    }

    @Test
    @DisplayName("getCurrentUserId returns user ID when authenticated")
    void getCurrentUserId_WhenAuthenticated_ReturnsUserId() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "test@example.com", Set.of("VIEWER"));

        UUID result = authorizationService.getCurrentUserId();

        assertThat(result).isEqualTo(userId);
    }

    // ========== getCurrentUserRoles Tests ==========

    @Test
    @DisplayName("getCurrentUserRoles returns empty set when not authenticated")
    void getCurrentUserRoles_WhenNotAuthenticated_ReturnsEmptySet() {
        Set<Role> roles = authorizationService.getCurrentUserRoles();
        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("getCurrentUserRoles returns parsed roles")
    void getCurrentUserRoles_WhenAuthenticated_ReturnsRoles() {
        setAuthenticatedUser(UUID.randomUUID(), "test@example.com", Set.of("CONTRIBUTOR", "EDITOR"));

        Set<Role> roles = authorizationService.getCurrentUserRoles();

        assertThat(roles).containsExactlyInAnyOrder(Role.CONTRIBUTOR, Role.EDITOR);
    }

    @Test
    @DisplayName("getCurrentUserRoles filters out null roles")
    void getCurrentUserRoles_WithInvalidRole_FiltersNull() {
        setAuthenticatedUser(UUID.randomUUID(), "test@example.com", Set.of("CONTRIBUTOR", "INVALID_ROLE"));

        Set<Role> roles = authorizationService.getCurrentUserRoles();

        assertThat(roles).containsExactly(Role.CONTRIBUTOR);
    }

    // ========== hasAnyRole Tests ==========

    @Test
    @DisplayName("hasAnyRole returns true when user has one of the roles")
    void hasAnyRole_WhenUserHasRole_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "test@example.com", Set.of("CONTRIBUTOR"));

        boolean result = authorizationService.hasAnyRole(Role.CONTRIBUTOR, Role.EDITOR);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasAnyRole returns false when user doesn't have any role")
    void hasAnyRole_WhenUserLacksRole_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "test@example.com", Set.of("VIEWER"));

        boolean result = authorizationService.hasAnyRole(Role.CONTRIBUTOR, Role.EDITOR);

        assertThat(result).isFalse();
    }

    // ========== isAdmin Tests ==========

    @Test
    @DisplayName("isAdmin returns true for SUPER_ADMIN")
    void isAdmin_WhenSuperAdmin_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "admin@example.com", Set.of("SUPER_ADMIN"));

        assertThat(authorizationService.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("isAdmin returns false for non-admin")
    void isAdmin_WhenNotAdmin_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("CONTRIBUTOR"));

        assertThat(authorizationService.isAdmin()).isFalse();
    }

    // ========== requireAnyRole Tests ==========

    @Test
    @DisplayName("requireAnyRole succeeds when user has role")
    void requireAnyRole_WhenUserHasRole_Succeeds() {
        setAuthenticatedUser(UUID.randomUUID(), "test@example.com", Set.of("EDITOR"));

        assertThatCode(() -> authorizationService.requireAnyRole(Role.EDITOR, Role.SUPER_ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireAnyRole throws when user lacks role")
    void requireAnyRole_WhenUserLacksRole_Throws() {
        setAuthenticatedUser(UUID.randomUUID(), "test@example.com", Set.of("VIEWER"));

        assertThatThrownBy(() -> authorizationService.requireAnyRole(Role.EDITOR, Role.SUPER_ADMIN))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Insufficient permissions");
    }

    // ========== isOwnerOrAdmin Tests ==========

    @Test
    @DisplayName("isOwnerOrAdmin returns true when user is admin")
    void isOwnerOrAdmin_WhenAdmin_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "admin@example.com", Set.of("SUPER_ADMIN"));

        boolean result = authorizationService.isOwnerOrAdmin(UUID.randomUUID());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isOwnerOrAdmin returns true when user is owner")
    void isOwnerOrAdmin_WhenOwner_ReturnsTrue() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "user@example.com", Set.of("CONTRIBUTOR"));

        boolean result = authorizationService.isOwnerOrAdmin(userId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isOwnerOrAdmin returns false when user is neither")
    void isOwnerOrAdmin_WhenNeither_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("CONTRIBUTOR"));

        boolean result = authorizationService.isOwnerOrAdmin(UUID.randomUUID());

        assertThat(result).isFalse();
    }

    // ========== Article Permission Tests ==========

    @Test
    @DisplayName("canCreateArticles returns true for CONTRIBUTOR")
    void canCreateArticles_WhenContributor_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("CONTRIBUTOR"));

        assertThat(authorizationService.canCreateArticles()).isTrue();
    }

    @Test
    @DisplayName("canCreateArticles returns false for VIEWER")
    void canCreateArticles_WhenViewer_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("VIEWER"));

        assertThat(authorizationService.canCreateArticles()).isFalse();
    }

    @Test
    @DisplayName("requireCanCreateArticles throws for VIEWER")
    void requireCanCreateArticles_WhenViewer_Throws() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("VIEWER"));

        assertThatThrownBy(() -> authorizationService.requireCanCreateArticles())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("CONTRIBUTOR or higher");
    }

    @Test
    @DisplayName("canViewArticle returns true for public article")
    void canViewArticle_WhenPublicArticle_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("VIEWER"));
        Article article = new Article(UUID.randomUUID(), "Title", "Content", UUID.randomUUID(), true, null, null);

        assertThat(authorizationService.canViewArticle(article)).isTrue();
    }

    @Test
    @DisplayName("canViewArticle returns false for VIEWER on private article")
    void canViewArticle_WhenViewerAndPrivate_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("VIEWER"));
        Article article = new Article(UUID.randomUUID(), "Title", "Content", UUID.randomUUID(), false, null, null);

        assertThat(authorizationService.canViewArticle(article)).isFalse();
    }

    @Test
    @DisplayName("canViewArticle returns true for CONTRIBUTOR on private article")
    void canViewArticle_WhenContributorAndPrivate_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("CONTRIBUTOR"));
        Article article = new Article(UUID.randomUUID(), "Title", "Content", UUID.randomUUID(), false, null, null);

        assertThat(authorizationService.canViewArticle(article)).isTrue();
    }

    @Test
    @DisplayName("canUpdateArticle returns true for article owner")
    void canUpdateArticle_WhenOwner_ReturnsTrue() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "user@example.com", Set.of("CONTRIBUTOR"));
        Article article = new Article(UUID.randomUUID(), "Title", "Content", userId, true, null, null);

        assertThat(authorizationService.canUpdateArticle(article)).isTrue();
    }

    @Test
    @DisplayName("canUpdateArticle returns false for non-owner CONTRIBUTOR")
    void canUpdateArticle_WhenNotOwner_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("CONTRIBUTOR"));
        Article article = new Article(UUID.randomUUID(), "Title", "Content", UUID.randomUUID(), true, null, null);

        assertThat(authorizationService.canUpdateArticle(article)).isFalse();
    }

    @Test
    @DisplayName("canDeleteArticle returns true for EDITOR owner")
    void canDeleteArticle_WhenEditorOwner_ReturnsTrue() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "user@example.com", Set.of("EDITOR"));
        Article article = new Article(UUID.randomUUID(), "Title", "Content", userId, true, null, null);

        assertThat(authorizationService.canDeleteArticle(article)).isTrue();
    }

    @Test
    @DisplayName("canDeleteArticle returns false for CONTRIBUTOR owner")
    void canDeleteArticle_WhenContributorOwner_ReturnsFalse() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "user@example.com", Set.of("CONTRIBUTOR"));
        Article article = new Article(UUID.randomUUID(), "Title", "Content", userId, true, null, null);

        assertThat(authorizationService.canDeleteArticle(article)).isFalse();
    }

    @Test
    @DisplayName("canDeleteArticle returns true for SUPER_ADMIN")
    void canDeleteArticle_WhenAdmin_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "admin@example.com", Set.of("SUPER_ADMIN"));
        Article article = new Article(UUID.randomUUID(), "Title", "Content", UUID.randomUUID(), true, null, null);

        assertThat(authorizationService.canDeleteArticle(article)).isTrue();
    }

    // ========== User Management Permission Tests ==========

    @Test
    @DisplayName("canManageUsers returns true for SUPER_ADMIN")
    void canManageUsers_WhenAdmin_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "admin@example.com", Set.of("SUPER_ADMIN"));

        assertThat(authorizationService.canManageUsers()).isTrue();
    }

    @Test
    @DisplayName("canManageUsers returns false for EDITOR")
    void canManageUsers_WhenEditor_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("EDITOR"));

        assertThat(authorizationService.canManageUsers()).isFalse();
    }

    @Test
    @DisplayName("canUpdateUser returns true for own profile")
    void canUpdateUser_WhenOwnProfile_ReturnsTrue() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "user@example.com", Set.of("VIEWER"));

        assertThat(authorizationService.canUpdateUser(userId)).isTrue();
    }

    @Test
    @DisplayName("canUpdateUser returns false for other user's profile")
    void canUpdateUser_WhenOtherProfile_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("CONTRIBUTOR"));

        assertThat(authorizationService.canUpdateUser(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("canAssignRoles returns true for SUPER_ADMIN")
    void canAssignRoles_WhenAdmin_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "admin@example.com", Set.of("SUPER_ADMIN"));

        assertThat(authorizationService.canAssignRoles()).isTrue();
    }

    @Test
    @DisplayName("canAssignRoles returns false for non-admin")
    void canAssignRoles_WhenNotAdmin_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("EDITOR"));

        assertThat(authorizationService.canAssignRoles()).isFalse();
    }

    // ========== Audit Log Permission Tests ==========

    @Test
    @DisplayName("canViewAuditLogs returns true for SUPER_ADMIN")
    void canViewAuditLogs_WhenAdmin_ReturnsTrue() {
        setAuthenticatedUser(UUID.randomUUID(), "admin@example.com", Set.of("SUPER_ADMIN"));

        assertThat(authorizationService.canViewAuditLogs()).isTrue();
    }

    @Test
    @DisplayName("canViewAuditLogs returns false for non-admin")
    void canViewAuditLogs_WhenNotAdmin_ReturnsFalse() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("EDITOR"));

        assertThat(authorizationService.canViewAuditLogs()).isFalse();
    }

    @Test
    @DisplayName("requireCanViewAuditLogs throws for non-admin")
    void requireCanViewAuditLogs_WhenNotAdmin_Throws() {
        setAuthenticatedUser(UUID.randomUUID(), "user@example.com", Set.of("EDITOR"));

        assertThatThrownBy(() -> authorizationService.requireCanViewAuditLogs())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SUPER_ADMIN");
    }
}
