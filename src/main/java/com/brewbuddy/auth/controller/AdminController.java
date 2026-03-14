package com.brewbuddy.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminDashboard(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "message", "Welcome to admin dashboard",
            "admin", jwt.getClaim("preferred_username")
        ));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        return ResponseEntity.ok(Map.of(
            "message", "List of all users",
            "totalUsers", 0
        ));
    }
}
