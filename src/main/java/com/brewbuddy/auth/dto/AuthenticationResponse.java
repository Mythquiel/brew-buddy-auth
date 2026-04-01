package com.brewbuddy.auth.dto;

public record AuthenticationResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    UserResponse user
) {
}
