package com.brewbuddy.auth.controller;

import com.brewbuddy.auth.dto.UserInfoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public ResponseEntity<UserInfoDto> getUserProfile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Set<String> roles = Set.of();

        if (realmAccess != null && realmAccess.get("roles") instanceof List) {
            roles = ((List<String>) realmAccess.get("roles")).stream()
                .collect(Collectors.toSet());
        }

        UserInfoDto userInfo = new UserInfoDto(
            jwt.getClaim("preferred_username"),
            jwt.getClaim("email"),
            jwt.getClaim("given_name"),
            jwt.getClaim("family_name"),
            roles
        );

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, String>> userDashboard(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "message", "Welcome to user dashboard",
            "user", jwt.getClaim("preferred_username")
        ));
    }
}
