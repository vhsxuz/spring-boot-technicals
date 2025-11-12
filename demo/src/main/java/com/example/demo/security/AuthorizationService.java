package com.example.demo.security;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.demo.domain.model.Article;

/**
 * Service for handling authorization checks across the application.
 * Provides centralized permission validation for RBAC enforcement.
 */
@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    /**
     * Get the currently authenticated user principal
     * @return UserPrincipal or null if not authenticated
     */
    public UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    /**
     * Get the current user's ID
     * @throws AccessDeniedException if not authenticated
     */
    public UUID getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        if (user == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return user.id();
    }

    /**
     * Get the current user's roles as Role enum set
     */
    public Set<Role> getCurrentUserRoles() {
        UserPrincipal user = getCurrentUser();
        if (user == null) {
            logger.debug("getCurrentUserRoles: No authenticated user");
            return Set.of();
        }
        Set<String> roleKeys = user.roles();
        logger.debug("getCurrentUserRoles: User {} has role keys: {}", user.id(), roleKeys);

        Set<Role> roles = roleKeys.stream()
                .map(Role::fromKey)
                .filter(r -> r != null)
                .collect(Collectors.toSet());

        logger.debug("getCurrentUserRoles: Parsed to Role enums: {}", roles);
        return roles;
    }

    /**
     * Check if current user has any of the specified roles
     */
    public boolean hasAnyRole(Role... roles) {
        Set<Role> userRoles = getCurrentUserRoles();
        for (Role role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user has admin privileges
     */
    public boolean isAdmin() {
        return hasAnyRole(Role.SUPER_ADMIN);
    }

    /**
     * Require that the current user has any of the specified roles
     * @throws AccessDeniedException if user doesn't have required role
     */
    public void requireAnyRole(Role... roles) {
        if (!hasAnyRole(roles)) {
            throw new AccessDeniedException("Insufficient permissions. Required role: " + String.join(" or ",
                java.util.Arrays.stream(roles).map(Role::getKey).toArray(String[]::new)));
        }
    }

    /**
     * Require that the current user is an admin
     * @throws AccessDeniedException if user is not admin
     */
    public void requireAdmin() {
        requireAnyRole(Role.SUPER_ADMIN);
    }

    /**
     * Check if current user is the owner of a resource or is an admin
     */
    public boolean isOwnerOrAdmin(UUID ownerId) {
        if (isAdmin()) {
            return true;
        }
        UserPrincipal user = getCurrentUser();
        return user != null && user.id().equals(ownerId);
    }

    /**
     * Require that the current user is the owner of a resource or is an admin
     * @throws AccessDeniedException if user is not owner or admin
     */
    public void requireOwnerOrAdmin(UUID ownerId, String resourceType) {
        if (!isOwnerOrAdmin(ownerId)) {
            throw new AccessDeniedException("You don't have permission to access this " + resourceType);
        }
    }

    // ========== Article-specific authorization ==========

    /**
     * Check if current user can create articles
     */
    public boolean canCreateArticles() {
        Set<Role> roles = getCurrentUserRoles();
        boolean canCreate = roles.stream().anyMatch(Role::canCreateArticles);
        logger.debug("canCreateArticles: User roles={}, canCreate={}", roles, canCreate);
        return canCreate;
    }

    /**
     * Require that the current user can create articles
     * @throws AccessDeniedException if user cannot create articles
     */
    public void requireCanCreateArticles() {
        logger.debug("requireCanCreateArticles: Checking permissions");
        if (!canCreateArticles()) {
            Set<Role> roles = getCurrentUserRoles();
            logger.warn("requireCanCreateArticles: Access denied. User roles: {}", roles);
            throw new AccessDeniedException("Insufficient permissions to create articles. Required role: CONTRIBUTOR or higher");
        }
        logger.debug("requireCanCreateArticles: Permission granted");
    }

    /**
     * Check if current user can update the given article
     * User can update if they are:
     * - Admin (SUPER_ADMIN)
     * - Owner of the article (EDITOR or CONTRIBUTOR)
     */
    public boolean canUpdateArticle(Article article) {
        if (isAdmin()) {
            return true;
        }
        Set<Role> roles = getCurrentUserRoles();
        boolean canEdit = roles.contains(Role.EDITOR) || roles.contains(Role.CONTRIBUTOR);
        return canEdit && isOwnerOrAdmin(article.authorId());
    }

    /**
     * Require that the current user can update the given article
     * @throws AccessDeniedException if user cannot update the article
     */
    public void requireCanUpdateArticle(Article article) {
        if (!canUpdateArticle(article)) {
            throw new AccessDeniedException("You don't have permission to update this article");
        }
    }

    /**
     * Check if current user can delete the given article
     * User can delete if they are:
     * - Admin (SUPER_ADMIN)
     * - Owner of the article AND has EDITOR role (CONTRIBUTOR cannot delete)
     */
    public boolean canDeleteArticle(Article article) {
        if (isAdmin()) {
            return true;
        }
        Set<Role> roles = getCurrentUserRoles();
        boolean hasDeleteRole = roles.stream().anyMatch(Role::canDeleteArticles);
        return hasDeleteRole && isOwnerOrAdmin(article.authorId());
    }

    /**
     * Require that the current user can delete the given article
     * @throws AccessDeniedException if user cannot delete the article
     */
    public void requireCanDeleteArticle(Article article) {
        if (!canDeleteArticle(article)) {
            throw new AccessDeniedException("You don't have permission to delete this article. Only SUPER_ADMIN or EDITOR (owner) can delete.");
        }
    }

    /**
     * Check if current user can view the given article
     * - VIEWER: only public articles
     * - CONTRIBUTOR/EDITOR: all articles (public + private)
     * - SUPER_ADMIN: all articles
     */
    public boolean canViewArticle(Article article) {
        if (isAdmin()) {
            return true;
        }
        Set<Role> roles = getCurrentUserRoles();

        // If article is public, anyone can view it
        if (article.isPublic()) {
            return true;
        }

        // Private articles: only CONTRIBUTOR/EDITOR/ADMIN or owner can view
        boolean canViewPrivate = roles.stream().anyMatch(Role::canViewAllArticles);
        return canViewPrivate || isOwnerOrAdmin(article.authorId());
    }

    /**
     * Require that the current user can view the given article
     * @throws AccessDeniedException if user cannot view the article
     */
    public void requireCanViewArticle(Article article) {
        if (!canViewArticle(article)) {
            throw new AccessDeniedException("You don't have permission to view this article");
        }
    }

    // ========== User management authorization ==========

    /**
     * Check if current user can manage other users (CRUD operations)
     * Only SUPER_ADMIN can manage users
     */
    public boolean canManageUsers() {
        return isAdmin();
    }

    /**
     * Require that the current user can manage users
     * @throws AccessDeniedException if user cannot manage users
     */
    public void requireCanManageUsers() {
        if (!canManageUsers()) {
            throw new AccessDeniedException("Insufficient permissions to manage users. Required role: SUPER_ADMIN");
        }
    }

    /**
     * Check if current user can update the specified user
     * Users can update their own profile, or SUPER_ADMIN can update anyone
     */
    public boolean canUpdateUser(UUID userId) {
        return isAdmin() || getCurrentUserId().equals(userId);
    }

    /**
     * Require that the current user can update the specified user
     * @throws AccessDeniedException if user cannot update the target user
     */
    public void requireCanUpdateUser(UUID userId) {
        if (!canUpdateUser(userId)) {
            throw new AccessDeniedException("You can only update your own profile");
        }
    }

    /**
     * Check if current user can assign roles
     * Only SUPER_ADMIN can assign roles
     */
    public boolean canAssignRoles() {
        return isAdmin();
    }

    /**
     * Require that the current user can assign roles
     * @throws AccessDeniedException if user cannot assign roles
     */
    public void requireCanAssignRoles() {
        if (!canAssignRoles()) {
            throw new AccessDeniedException("Insufficient permissions to assign roles. Required role: SUPER_ADMIN");
        }
    }

    // ========== Audit log authorization ==========

    /**
     * Check if current user can view audit logs
     * Only SUPER_ADMIN can view audit logs
     */
    public boolean canViewAuditLogs() {
        return isAdmin();
    }

    /**
     * Require that the current user can view audit logs
     * @throws AccessDeniedException if user cannot view audit logs
     */
    public void requireCanViewAuditLogs() {
        if (!canViewAuditLogs()) {
            throw new AccessDeniedException("Insufficient permissions to view audit logs. Required role: SUPER_ADMIN");
        }
    }
}
