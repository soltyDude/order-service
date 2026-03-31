package com.example.orderservice.api.dto.response;

import java.util.UUID;

public record OrderStatusDto(
        UUID orderId,
        String status,
        String paymentStatus,
        String reservationId
) {}