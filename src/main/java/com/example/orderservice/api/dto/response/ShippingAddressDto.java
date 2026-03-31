package com.example.orderservice.api.dto.response;

public record ShippingAddressDto(
        String street,
        String city,
        String zipCode,
        String country
) {}