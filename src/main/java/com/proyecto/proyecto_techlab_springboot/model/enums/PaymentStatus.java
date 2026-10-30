package com.proyecto.proyecto_techlab_springboot.model.enums;

public enum PaymentStatus {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    REEMBOLSO_PENDIENTE,    // Reintegro autorizado internamente, pendiente de impacto bancario/pasarela
    REEMBOLSADO             // Fondos devueltos al cliente
}