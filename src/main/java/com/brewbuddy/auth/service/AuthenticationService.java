package com.brewbuddy.auth.service;

import com.brewbuddy.auth.dto.AuthenticationResponse;
import com.brewbuddy.auth.dto.LoginRequest;
import com.brewbuddy.auth.dto.LogoutRequest;
import com.brewbuddy.auth.dto.RefreshTokenRequest;
import com.brewbuddy.auth.dto.RegisterRequest;
import com.brewbuddy.auth.dto.UserResponse;
import com.brewbuddy.auth.entity.User;
import com.brewbuddy.auth.entity.UserRole;
import com.brewbuddy.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.expiration-ms}")
    private Long expirationMs;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username()) ||
            userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Registration failed. Username or email may already be in use.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(false);

        // Add default USER role
        UserRole userRole = new UserRole();
        userRole.setRoleName("USER");
        userRole.setUser(user);
        user.getRoles().add(userRole);

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expirationMs / 1000,
                convertToUserResponse(savedUser)
        );
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expirationMs / 1000,
                convertToUserResponse(user)
        );
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String username = jwtService.extractUsername(request.refreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate new tokens (implements sliding window - resets the idle timer)
        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // Blacklist the old refresh token to prevent reuse
        tokenBlacklistService.blacklistToken(request.refreshToken());

        return new AuthenticationResponse(
                accessToken,
                newRefreshToken,
                "Bearer",
                expirationMs / 1000,
                convertToUserResponse(user)
        );
    }

    public void logout(LogoutRequest request) {
        tokenBlacklistService.blacklistBothTokens(request.accessToken(), request.refreshToken());
    }

    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmailVerified(),
                user.getEnabled(),
                user.getRoles().stream()
                        .map(UserRole::getRoleName)
                        .collect(Collectors.toSet()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
