package com.example.orderservice.api.dto.response;

import java.util.UUID;

public record OrderCancelDto(
        UUID orderId,
        String status,
        String cancelReason,
        CompensationsDto compensations
) {}