package com.example.orderservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressRequest(
        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Zip code is required")
        String zipCode,

        @NotBlank(message = "Country is required")
        String country
) {}