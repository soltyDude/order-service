package com.example.orderservice.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Object details
) {
    public ErrorResponse(int status, String error, String message, String path, String correlationId) {
        this(Instant.now(), status, error, message, path, correlationId, null);
    }

    public ErrorResponse(int status, String error, String message, String path, String correlationId, Object details) {
        this(Instant.now(), status, error, message, path, correlationId, details);
    }
}