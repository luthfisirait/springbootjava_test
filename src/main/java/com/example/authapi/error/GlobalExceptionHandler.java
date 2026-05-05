package com.example.authapi.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        return buildResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "Required form field '" + ex.getParameterName() + "' is missing",
                request
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "Use Content-Type: application/x-www-form-urlencoded",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnhandled(Exception ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Unexpected server error",
                request
        );
    }

    private static ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                false,
                code,
                message,
                status.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(error);
    }
}
