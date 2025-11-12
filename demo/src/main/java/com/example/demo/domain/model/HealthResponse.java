package com.example.demo.domain.model;

public record HealthResponse(
  String system_status, 
  String status_message
) {}