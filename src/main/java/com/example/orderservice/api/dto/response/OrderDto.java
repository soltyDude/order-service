package com.example.orderservice.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID orderId,
        UUID userId,
        String status,
        BigDecimal totalAmount,
        int itemCount,
        List<OrderItemDto> items,
        String paymentId,
        String paymentStatus,
        String reservationId,
        String paymentMethod,
        ShippingAddressDto shippingAddress,
        String cancelReason,
        Instant createdAt,
        Instant updatedAt
) {}