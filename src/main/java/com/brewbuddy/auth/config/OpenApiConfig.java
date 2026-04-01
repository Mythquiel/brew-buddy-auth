package com.brewbuddy.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Brew Buddy Auth API",
        version = "1.0.0",
        description = """
            Authentication and user management service built with Spring Boot 3.4 and JWT tokens.

            ## Features
            - JWT-based authentication (access + refresh tokens)
            - User registration and login
            - Role-based access control (USER, ADMIN)
            - User CRUD operations
            - Profile management
            - Password management

            ## Authentication
            Most endpoints require authentication. Include the JWT token in the Authorization header:
            ```
            Authorization: Bearer {your-access-token}
            ```

            ## Getting Started
            1. Register a new user: `POST /api/auth/register`
            2. Login to get tokens: `POST /api/auth/login`
            3. Use the access token in the Authorization header for protected endpoints
            4. Refresh your token when it expires: `POST /api/auth/refresh`
            """,
        contact = @Contact(
            name = "Mythquiel",
            email = "ma.switala@gmail.com"
        )
    ),
    servers = {
        @Server(
            description = "Local Development",
            url = "http://localhost:8081"
        ),
        @Server(
            description = "Production",
            url = "https://brew-buddy-auth.fly.dev"
        )
    }
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter your JWT token obtained from /api/auth/login or /api/auth/register"
)
public class OpenApiConfig {
}
