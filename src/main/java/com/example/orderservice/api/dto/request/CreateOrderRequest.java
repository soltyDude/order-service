package com.example.orderservice.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid OrderItemRequest> items,

        @Valid
        @NotNull(message = "Shipping address is required")
        ShippingAddressRequest shippingAddress,

        @NotNull(message = "Payment method is required")
        String paymentMethod
) {}