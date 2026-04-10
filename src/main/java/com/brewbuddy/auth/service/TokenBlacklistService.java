package com.brewbuddy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    /**
     * Add a token to the blacklist. The token will be stored in Redis
     * with a TTL matching its expiration time.
     *
     * @param token The JWT token to blacklist
     */
    public void blacklistToken(String token) {
        try {
            Date expiration = jwtService.extractExpiration(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            // Only blacklist if the token hasn't already expired
            if (ttl > 0) {
                String key = BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttl));
                log.info("Token blacklisted successfully with TTL: {} ms", ttl);
            } else {
                log.debug("Token already expired, no need to blacklist");
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

    /**
     * Check if a token is blacklisted.
     *
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check token blacklist status", e);
            // Fail open: if Redis is down, don't block all requests
            return false;
        }
    }

    /**
     * Blacklist both access and refresh tokens.
     *
     * @param accessToken The access token to blacklist
     * @param refreshToken The refresh token to blacklist
     */
    public void blacklistBothTokens(String accessToken, String refreshToken) {
        blacklistToken(accessToken);
        if (refreshToken != null && !refreshToken.isEmpty()) {
            blacklistToken(refreshToken);
        }
    }
}
