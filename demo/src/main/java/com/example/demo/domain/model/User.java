package com.example.demo.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record User(
    UUID id,
    String fullname,
    String username,
    String email,
    Boolean isEmailVerified,
    OffsetDateTime blockedUntil,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}