package com.brewbuddy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    Boolean temporary
) {
    public ChangePasswordRequest {
        if (temporary == null) temporary = false;
    }
}
