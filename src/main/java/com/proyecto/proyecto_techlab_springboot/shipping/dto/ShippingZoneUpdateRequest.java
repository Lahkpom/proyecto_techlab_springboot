package com.proyecto.proyecto_techlab_springboot.shipping.dto;

import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record ShippingZoneUpdateRequest(
        @NotBlank(message = "El nombre de la zona es obligatorio")
        String zoneName,

        String description,

        @NotNull(message = "El costo base es obligatorio")
        @DecimalMin(value = "0.00", message = "El costo base no puede ser negativo")
        BigDecimal baseCost,

        @NotBlank(message = "El plazo estimado es obligatorio")
        String estimatedDays,

        List<
                @NotBlank(message = "Los códigos postales no pueden estar vacíos")
                @Pattern(regexp = "^\\d{4}$", message = "Cada código postal debe contener cuatro dígitos")
                String
                > postalCodes,

        RowStatus rowStatus
) {}
