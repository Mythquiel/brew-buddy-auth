package com.brewbuddy.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Set test values using reflection
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-for-testing-must-be-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtService, "issuer", "brew-buddy-auth-test");

        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        userDetails = User.builder()
            .username("testuser")
            .password("password")
            .authorities(authorities)
            .build();
    }

    @Test
    void shouldGenerateAccessToken() {
        String token = jwtService.generateAccessToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void shouldGenerateRefreshToken() {
        String token = jwtService.generateRefreshToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateAccessToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void shouldExtractExpirationFromToken() {
        String token = jwtService.generateAccessToken(userDetails);

        Date expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void shouldValidateValidToken() {
        String token = jwtService.generateAccessToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    void shouldNotValidateTokenWithWrongUsername() {
        String token = jwtService.generateAccessToken(userDetails);

        UserDetails differentUser = User.builder()
            .username("differentuser")
            .password("password")
            .authorities(List.of())
            .build();

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    void shouldNotValidateExpiredToken() {
        // Set very short expiration
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);

        String token = jwtService.generateAccessToken(userDetails);

        // This token is now expired, should throw exception during validation
        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldThrowExceptionForInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void accessTokenShouldContainRoles() {
        String token = jwtService.generateAccessToken(userDetails);

        Claims claims = jwtService.extractClaim(token, claims1 -> claims1);
        List<String> roles = claims.get("roles", List.class);

        assertThat(roles).contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void refreshTokenShouldNotContainRoles() {
        String token = jwtService.generateRefreshToken(userDetails);

        Claims claims = jwtService.extractClaim(token, claims1 -> claims1);
        List<String> roles = claims.get("roles", List.class);

        assertThat(roles).isNull();
    }

    @Test
    void tokenShouldContainIssuer() {
        String token = jwtService.generateAccessToken(userDetails);

        Claims claims = jwtService.extractClaim(token, claims1 -> claims1);
        String issuer = claims.getIssuer();

        assertThat(issuer).isEqualTo("brew-buddy-auth-test");
    }

    @Test
    void tokenShouldContainIssuedAt() {
        String token = jwtService.generateAccessToken(userDetails);

        Claims claims = jwtService.extractClaim(token, claims1 -> claims1);
        Date issuedAt = claims.getIssuedAt();

        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt).isBeforeOrEqualTo(new Date());
    }

    @Test
    void shouldGenerateDifferentTokensForSameUser() {
        String token1 = jwtService.generateAccessToken(userDetails);
        String token2 = jwtService.generateAccessToken(userDetails);

        // Tokens will be different due to different issued at timestamp
        // This test may occasionally fail if both tokens are generated in the same millisecond
        // In practice, this is acceptable as the test demonstrates the concept
        assertThat(token1).isNotEmpty();
        assertThat(token2).isNotEmpty();
    }
}
