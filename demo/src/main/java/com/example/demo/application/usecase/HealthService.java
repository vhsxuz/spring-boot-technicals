package com.example.demo.application.usecase;

import org.springframework.stereotype.Service;

import com.example.demo.domain.model.HealthResponse;
import com.example.demo.domain.port.in.HealthUseCase;

@Service
public class HealthService implements HealthUseCase {
  @Override
  public HealthResponse healthCheck() {
    return new HealthResponse("ALIVE", "System is healthy");
  }
}