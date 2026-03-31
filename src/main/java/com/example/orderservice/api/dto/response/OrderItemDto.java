package com.example.orderservice.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID productId,
        int quantity,
        BigDecimal price,
        BigDecimal subtotal
) {}