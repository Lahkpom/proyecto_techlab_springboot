package com.proyecto.proyecto_techlab_springboot.sale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record DevMercadoPagoSimulateRequest(
        @NotNull UUID saleId,
        
        @NotNull
        @Pattern(regexp = "approved|pending|rejected")
        String outcome
) {}
