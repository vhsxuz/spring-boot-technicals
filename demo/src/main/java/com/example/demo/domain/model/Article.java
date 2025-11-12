package com.example.demo.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Article(
  UUID id,
  String title,
  String content,
  UUID authorId,
  boolean isPublic,
  OffsetDateTime createdAt,
  OffsetDateTime updatedAt
) {}