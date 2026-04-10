package com.brewbuddy.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Logout request containing tokens to invalidate")
public record LogoutRequest(
        @NotBlank(message = "Access token is required")
        @Schema(description = "The access token to invalidate", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "The refresh token to invalidate (optional)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken
) {}
