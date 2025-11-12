package com.example.demo.domain.model;

public record ApiResponse<T>(int code, String status, String message, T data) {
  // Factories
  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<>(200, "OK", message, data);
  }
  public static <T> ApiResponse<T> created(String message, T data) {
    return new ApiResponse<>(201, "Created", message, data);
  }
  public static <T> ApiResponse<T> noContent(String message) {
    return new ApiResponse<>(204, "No Content", message, null);
  }
  public static <T> ApiResponse<T> error(int code, String status, String message) {
    return new ApiResponse<>(code, status, message, null);
  }
}
