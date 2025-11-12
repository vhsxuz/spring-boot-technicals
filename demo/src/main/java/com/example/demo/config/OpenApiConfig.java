package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot Technical Demo API")
                        .description("""
                                RESTful API with comprehensive security features including:
                                - JWT-based authentication with MFA (OTP via email)
                                - Role-Based Access Control (RBAC) with 4 roles: SUPER_ADMIN, EDITOR, CONTRIBUTOR, VIEWER
                                - Comprehensive audit logging
                                - Rate limiting and account blocking
                                - Article management with public/private visibility

                                **Authentication Flow:**
                                1. Register: POST /auth/register → Receive OTP via email
                                2. Verify OTP: POST /auth/verify-otp → Get pending JWT token
                                3. Login: POST /auth/login → Receive OTP via email
                                4. Verify OTP: POST /auth/verify-otp → Get access JWT token
                                5. Use token: Add "Authorization: Bearer {token}" header to requests

                                **Role Permissions:**
                                - VIEWER: View public articles only
                                - CONTRIBUTOR: Create/update own articles, view all articles
                                - EDITOR: CRUD own articles, view all articles
                                - SUPER_ADMIN: Full access to users, articles, and audit logs
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(contextPath)
                                .description("Local Development Server")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from /auth/login and /auth/verify-otp endpoints")));
    }
}
