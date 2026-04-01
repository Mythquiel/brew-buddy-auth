package com.brewbuddy.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String firstName,
    String lastName,
    Boolean emailVerified,
    Boolean enabled,
    Set<String> roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
