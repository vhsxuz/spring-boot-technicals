package com.example.demo.security;

/**
 * Enum representing application roles for RBAC.
 *
 * Roles define what operations a user can perform:
 * - SUPER_ADMIN: Full system access, can CRUD users, all articles, view all audit logs
 * - EDITOR: Can CRUD own articles, view all articles
 * - CONTRIBUTOR: Can create/update own articles (no delete), view all articles
 * - VIEWER: Can only view public articles (default role)
 */
public enum Role {
    SUPER_ADMIN("SUPER_ADMIN"),
    EDITOR("EDITOR"),
    CONTRIBUTOR("CONTRIBUTOR"),
    VIEWER("VIEWER");

    private final String key;

    Role(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Parse role from string key (case-insensitive)
     */
    public static Role fromKey(String key) {
        if (key == null) return null;
        for (Role role : values()) {
            if (role.key.equalsIgnoreCase(key)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Check if this role has admin privileges
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN;
    }

    /**
     * Check if this role can create articles
     */
    public boolean canCreateArticles() {
        return this == SUPER_ADMIN || this == EDITOR || this == CONTRIBUTOR;
    }

    /**
     * Check if this role can delete articles (only own articles)
     */
    public boolean canDeleteArticles() {
        return this == SUPER_ADMIN || this == EDITOR;
    }

    /**
     * Check if this role can view all articles (including private)
     */
    public boolean canViewAllArticles() {
        return this == SUPER_ADMIN || this == EDITOR || this == CONTRIBUTOR;
    }
}
