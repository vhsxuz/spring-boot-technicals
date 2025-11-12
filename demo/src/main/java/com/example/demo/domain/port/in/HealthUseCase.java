package com.example.demo.domain.port.in;

import com.example.demo.domain.model.HealthResponse;

public interface HealthUseCase {
  HealthResponse healthCheck();
}
