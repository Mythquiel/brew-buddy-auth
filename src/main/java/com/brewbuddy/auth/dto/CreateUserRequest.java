package com.brewbuddy.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    String firstName,

    String lastName,

    Boolean emailVerified,

    Boolean enabled,

    Set<String> roles
) {
    public CreateUserRequest {
        if (emailVerified == null) emailVerified = false;
        if (enabled == null) enabled = true;
        if (roles == null) roles = Set.of("USER");
    }
}
