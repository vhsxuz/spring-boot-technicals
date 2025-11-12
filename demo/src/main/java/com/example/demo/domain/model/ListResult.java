package com.example.demo.domain.model;

import java.util.List;

public record ListResult<T>(List<T> items, Meta meta) {
  public record Meta(long total, int limit, int offset) {}
}
