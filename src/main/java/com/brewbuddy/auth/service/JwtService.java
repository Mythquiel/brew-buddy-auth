package com.brewbuddy.auth.service;

import com.brewbuddy.auth.entity.User;
import com.brewbuddy.auth.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private Long expirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private Long refreshExpirationMs;

    @Value("${jwt.issuer}")
    private String issuer;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate access token with User entity (includes ID, username, email, roles)
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles().stream()
                .map(UserRole::getRoleName)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toList()));

        // Use user ID as subject
        return generateToken(claims, user.getId().toString(), expirationMs);
    }

    /**
     * Generate access token with UserDetails (legacy method for backward compatibility)
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return generateToken(claims, userDetails.getUsername(), expirationMs);
    }

    /**
     * Generate refresh token with User entity
     */
    public String generateRefreshToken(User user) {
        return generateToken(new HashMap<>(), user.getId().toString(), refreshExpirationMs);
    }

    /**
     * Generate refresh token with UserDetails (legacy method for backward compatibility)
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails.getUsername(), refreshExpirationMs);
    }

    private String generateToken(Map<String, Object> extraClaims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract user ID from token (subject is now UUID)
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract username from token (for legacy tokens, subject is username)
     * For new tokens, username is in claims
     */
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        // Try to get username from claims first (new format)
        String username = claims.get("username", String.class);
        if (username != null) {
            return username;
        }
        // Fallback to subject for legacy tokens
        return claims.getSubject();
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
