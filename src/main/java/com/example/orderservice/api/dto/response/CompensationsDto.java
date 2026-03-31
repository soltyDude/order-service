package com.example.orderservice.api.dto.response;

public record CompensationsDto(
        boolean refundInitiated,
        boolean stockReleaseInitiated
) {}