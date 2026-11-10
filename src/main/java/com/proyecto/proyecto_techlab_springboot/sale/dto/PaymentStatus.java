package com.proyecto.proyecto_techlab_springboot.sale.dto;

public enum PaymentStatus {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    REEMBOLSO_PENDIENTE,    // Reintegro autorizado internamente, pendiente de impacto bancario/pasarela
    REEMBOLSADO             // Fondos devueltos al cliente
}