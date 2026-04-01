package com.brewbuddy.auth.controller;

import com.brewbuddy.auth.dto.UserInfoDto;
import com.brewbuddy.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserInfoDto> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        var user = userService.getUserByUsername(userDetails.getUsername());

        UserInfoDto userInfo = new UserInfoDto(
            user.username(),
            user.email(),
            user.firstName(),
            user.lastName(),
            user.roles()
        );

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, String>> userDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
            "message", "Welcome to user dashboard",
            "user", userDetails.getUsername()
        ));
    }
}
