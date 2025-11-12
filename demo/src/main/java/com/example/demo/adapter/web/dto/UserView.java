package com.example.demo.adapter.web.dto;

import java.util.List;
import java.util.UUID;

import com.example.demo.domain.model.User;

public record UserView(
    User user,
    List<RoleItem> roles
) {
  public record RoleItem(UUID id, String roleKey, String name) {}
}
