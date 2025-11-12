package com.example.demo.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Role Enum Unit Tests")
class RoleTest {

    @Test
    @DisplayName("All roles have correct keys")
    void allRoles_HaveCorrectKeys() {
        assertThat(Role.SUPER_ADMIN.getKey()).isEqualTo("SUPER_ADMIN");
        assertThat(Role.EDITOR.getKey()).isEqualTo("EDITOR");
        assertThat(Role.CONTRIBUTOR.getKey()).isEqualTo("CONTRIBUTOR");
        assertThat(Role.VIEWER.getKey()).isEqualTo("VIEWER");
    }

    @ParameterizedTest
    @CsvSource({
            "SUPER_ADMIN, SUPER_ADMIN",
            "super_admin, SUPER_ADMIN",
            "EDITOR, EDITOR",
            "editor, EDITOR",
            "CONTRIBUTOR, CONTRIBUTOR",
            "contributor, CONTRIBUTOR",
            "VIEWER, VIEWER",
            "viewer, VIEWER"
    })
    @DisplayName("fromKey returns correct role (case-insensitive)")
    void fromKey_WithValidKey_ReturnsRole(String input, String expectedRoleKey) {
        Role role = Role.fromKey(input);
        assertThat(role).isNotNull();
        assertThat(role.getKey()).isEqualTo(expectedRoleKey);
    }

    @Test
    @DisplayName("fromKey returns null for invalid key")
    void fromKey_WithInvalidKey_ReturnsNull() {
        assertThat(Role.fromKey("INVALID_ROLE")).isNull();
    }

    @Test
    @DisplayName("fromKey returns null for null input")
    void fromKey_WithNull_ReturnsNull() {
        assertThat(Role.fromKey(null)).isNull();
    }

    @Test
    @DisplayName("isAdmin returns true only for SUPER_ADMIN")
    void isAdmin_OnlySuperAdminReturnsTrue() {
        assertThat(Role.SUPER_ADMIN.isAdmin()).isTrue();
        assertThat(Role.EDITOR.isAdmin()).isFalse();
        assertThat(Role.CONTRIBUTOR.isAdmin()).isFalse();
        assertThat(Role.VIEWER.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("canCreateArticles returns correct values")
    void canCreateArticles_ReturnsCorrectValues() {
        assertThat(Role.SUPER_ADMIN.canCreateArticles()).isTrue();
        assertThat(Role.EDITOR.canCreateArticles()).isTrue();
        assertThat(Role.CONTRIBUTOR.canCreateArticles()).isTrue();
        assertThat(Role.VIEWER.canCreateArticles()).isFalse();
    }

    @Test
    @DisplayName("canDeleteArticles returns correct values")
    void canDeleteArticles_ReturnsCorrectValues() {
        assertThat(Role.SUPER_ADMIN.canDeleteArticles()).isTrue();
        assertThat(Role.EDITOR.canDeleteArticles()).isTrue();
        assertThat(Role.CONTRIBUTOR.canDeleteArticles()).isFalse();
        assertThat(Role.VIEWER.canDeleteArticles()).isFalse();
    }

    @Test
    @DisplayName("canViewAllArticles returns correct values")
    void canViewAllArticles_ReturnsCorrectValues() {
        assertThat(Role.SUPER_ADMIN.canViewAllArticles()).isTrue();
        assertThat(Role.EDITOR.canViewAllArticles()).isTrue();
        assertThat(Role.CONTRIBUTOR.canViewAllArticles()).isTrue();
        assertThat(Role.VIEWER.canViewAllArticles()).isFalse();
    }

    @Test
    @DisplayName("SUPER_ADMIN has all permissions")
    void superAdmin_HasAllPermissions() {
        assertThat(Role.SUPER_ADMIN.isAdmin()).isTrue();
        assertThat(Role.SUPER_ADMIN.canCreateArticles()).isTrue();
        assertThat(Role.SUPER_ADMIN.canDeleteArticles()).isTrue();
        assertThat(Role.SUPER_ADMIN.canViewAllArticles()).isTrue();
    }

    @Test
    @DisplayName("VIEWER has minimal permissions")
    void viewer_HasMinimalPermissions() {
        assertThat(Role.VIEWER.isAdmin()).isFalse();
        assertThat(Role.VIEWER.canCreateArticles()).isFalse();
        assertThat(Role.VIEWER.canDeleteArticles()).isFalse();
        assertThat(Role.VIEWER.canViewAllArticles()).isFalse();
    }

    @Test
    @DisplayName("CONTRIBUTOR can create but not delete")
    void contributor_CanCreateButNotDelete() {
        assertThat(Role.CONTRIBUTOR.canCreateArticles()).isTrue();
        assertThat(Role.CONTRIBUTOR.canDeleteArticles()).isFalse();
        assertThat(Role.CONTRIBUTOR.canViewAllArticles()).isTrue();
    }

    @Test
    @DisplayName("EDITOR can create and delete")
    void editor_CanCreateAndDelete() {
        assertThat(Role.EDITOR.canCreateArticles()).isTrue();
        assertThat(Role.EDITOR.canDeleteArticles()).isTrue();
        assertThat(Role.EDITOR.canViewAllArticles()).isTrue();
        assertThat(Role.EDITOR.isAdmin()).isFalse();
    }
}
