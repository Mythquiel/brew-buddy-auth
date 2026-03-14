package com.brewbuddy.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "username", jwt.getClaim("preferred_username"),
            "expiresAt", jwt.getExpiresAt()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of(
            "message", "Logged out successfully"
        ));
    }
}
