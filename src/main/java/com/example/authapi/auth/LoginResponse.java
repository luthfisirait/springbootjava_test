package com.example.authapi.auth;

public record LoginResponse(
        boolean success,
        Long userId,
        String email,
        String token,
        String tokenType,
        long expiresIn
) {
}
