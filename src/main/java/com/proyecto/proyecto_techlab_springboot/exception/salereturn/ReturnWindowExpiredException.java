package com.proyecto.proyecto_techlab_springboot.exception.salereturn;

import java.time.Instant;
import java.util.UUID;

public class ReturnWindowExpiredException extends RuntimeException {
    public ReturnWindowExpiredException(UUID saleId, Instant deliveredAt, int allowedDays) {
        super(
                "La ventana legal de devolución (%d días corridos) ha expirado para la orden { ID: %s }. Fecha de entrega: %s"
                        .formatted(allowedDays, saleId, deliveredAt)
        );
    }
}