package com.example.orderservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    private String street;
    private String city;
    private String zipCode;
    private String country;
}