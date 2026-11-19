package com.proyecto.proyecto_techlab_springboot.shipping.dto;

public record ShippingQuoteRequest(
        boolean isPickup,
        String province,
        String locality,
        String postalCode
) {}
