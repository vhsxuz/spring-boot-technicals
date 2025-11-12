package com.example.demo.security;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
  private final String issuer;
  private final byte[] secretKey;
  private final long accessTokenValidityMinutes;

  public JwtService(Environment env) {
    this.issuer = env.getProperty("security.jwt.issuer", "example.com");
    String key = env.getProperty("security.jwt.secret-key", "default-secret-key-please-change");
    this.secretKey = key.getBytes();
    this.accessTokenValidityMinutes = env.getProperty("security.jwt.access-token-validity-minutes", Long.class, 60L);
  }

  public String issueAccess(String subject, Map<String, Object> claims) {
    Instant now = Instant.now();
    return Jwts.builder()
        .setIssuer(issuer)
        .setSubject(subject)
        .addClaims(claims)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plusSeconds(accessTokenValidityMinutes * 60)))
        .signWith(Keys.hmacShaKeyFor(secretKey), SignatureAlgorithm.HS256)
        .compact();
  }

  public Jws<Claims> parse(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(Keys.hmacShaKeyFor(secretKey))
        .requireIssuer(issuer)
        .build()
        .parseClaimsJws(token);
  }
}
