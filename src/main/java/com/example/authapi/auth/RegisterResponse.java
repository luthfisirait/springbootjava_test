package com.example.authapi.auth;

public record RegisterResponse(
        boolean success,
        Long userId,
        String email,
        String message
) {
}
