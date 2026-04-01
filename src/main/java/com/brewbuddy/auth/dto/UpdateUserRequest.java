package com.brewbuddy.auth.dto;

import jakarta.validation.constraints.Email;

import java.util.Set;

public record UpdateUserRequest(
    String username,

    @Email(message = "Email must be valid")
    String email,

    String firstName,

    String lastName,

    Boolean emailVerified,

    Boolean enabled,

    Set<String> roles
) {
}
