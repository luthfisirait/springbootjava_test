package com.example.authapi.auth;

public record IssuedToken(String token, long expiresIn) {
}
