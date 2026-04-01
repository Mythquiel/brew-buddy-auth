package com.brewbuddy.auth.exception;

import io.jsonwebtoken.JwtException;

public class TokenExpiredException extends JwtException {
    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
