package com.example.orderservice.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryDto(
        UUID orderId,
        String status,
        BigDecimal totalAmount,
        int itemCount,
        String paymentMethod,
        Instant createdAt
) {}