// security/SecurityConfig.java
package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.adapter.persistence.user.UserJpaRepository;
import com.example.demo.adapter.user_role.UserRoleJpaRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // Enable @PreAuthorize annotations
public class SecurityConfig {

  private final JwtService jwtService;
  private final UserJpaRepository userRepository;
  private final UserRoleJpaRepository userRoleRepository;

  public SecurityConfig(JwtService jwtService, UserJpaRepository userRepository,
                        UserRoleJpaRepository userRoleRepository) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public JwtAuthFilter jwtAuthFilter() {
    return new JwtAuthFilter(jwtService, userRepository, userRoleRepository);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize
            // Health check endpoints - public access
            .requestMatchers("/health/**").permitAll()

            // Swagger/OpenAPI documentation - public access
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

            // Public auth endpoints - anyone can register, login, verify OTP
            .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/verify-otp").permitAll()

            // Auth endpoints requiring authentication
            .requestMatchers("/auth/me").authenticated()
            .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()

            // User management - SUPER_ADMIN only (via controller authorization)
            .requestMatchers("/users/**").authenticated()

            // Article endpoints - all require authentication (role-based enforced in controller)
            .requestMatchers("/articles/**").authenticated()

            // Audit logs - SUPER_ADMIN only
            .requestMatchers("/audit-logs/**").authenticated()

            // All other endpoints require authentication
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}
