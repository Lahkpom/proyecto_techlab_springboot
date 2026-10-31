package com.proyecto.proyecto_techlab_springboot.exception.salereturn;

import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;

import java.util.UUID;

public class SaleNotEligibleForReturnException extends RuntimeException {
    public SaleNotEligibleForReturnException(UUID saleId, SaleStatus currentStatus) {
        super(
                "La orden { ID: %s } no es elegible para devolución. Estado actual: %s. Debe encontrarse en estado ENTREGADO."
                        .formatted(saleId, currentStatus)
        );
    }

    public SaleNotEligibleForReturnException(String message) {
        super(message);
    }
}