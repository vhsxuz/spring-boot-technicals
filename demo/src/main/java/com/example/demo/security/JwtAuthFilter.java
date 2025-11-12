// security/JwtAuthFilter.java
package com.example.demo.security;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import com.example.demo.adapter.persistence.user.UserJpaRepository;
import com.example.demo.adapter.user_role.UserRoleJpaRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.GenericFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class JwtAuthFilter extends GenericFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

  private final JwtService jwt;
  private final UserJpaRepository users;
  private final UserRoleJpaRepository userRoles;

  public JwtAuthFilter(JwtService jwt, UserJpaRepository users, UserRoleJpaRepository userRoles) {
    this.jwt = jwt; this.users = users; this.userRoles = userRoles;
  }

  @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    String header = req.getHeader("Authorization");
    if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        var jws = jwt.parse(token);
        Claims claims = jws.getBody();
        if ("pending".equals(claims.get("mfa"))) {
          logger.debug("Token has pending MFA, not authenticating");
          // don't authenticate pending MFA
        } else {
          UUID userId = UUID.fromString(claims.getSubject());
          logger.debug("JWT subject userId: {}", userId);

          var userOpt = users.findById(userId);
          if (userOpt.isPresent()) {
            var u = userOpt.get();
            Set<String> roles = userRoles.findRoleKeysByUserId(userId);
            logger.debug("User {} loaded with roles: {}", userId, roles);

            var principal = new UserPrincipal(userId, u.getEmail(), u.getPasswordHash(), roles);
            var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            logger.debug("Authentication successful for user: {}", userId);
          } else {
            logger.warn("User not found in database for userId: {}", userId);
          }
        }
      } catch (Exception e) {
        logger.error("JWT authentication failed: {}", e.getMessage(), e);
        /* fall through unauthenticated */
      }
    }
    chain.doFilter(request, response);
  }
}
