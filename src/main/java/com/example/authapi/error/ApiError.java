package com.example.authapi.error;

import java.time.Instant;

public record ApiError(
        boolean success,
        String code,
        String message,
        int status,
        String path,
        Instant timestamp
) {
}
