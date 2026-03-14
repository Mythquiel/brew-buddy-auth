package com.brewbuddy.auth.dto;

import java.util.Set;

public record UserInfoDto(
    String username,
    String email,
    String firstName,
    String lastName,
    Set<String> roles
) {
}
