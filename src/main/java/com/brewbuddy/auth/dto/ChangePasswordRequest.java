package com.brewbuddy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[@#$%^&+=!]).*$",
        message = "Password must contain at least one digit and one special character (@#$%^&+=!)"
    )
    String password,

    Boolean temporary
) {
    public ChangePasswordRequest {
        if (temporary == null) temporary = false;
    }
}
