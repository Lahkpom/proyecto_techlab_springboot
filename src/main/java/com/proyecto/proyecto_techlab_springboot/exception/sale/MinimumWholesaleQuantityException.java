package com.proyecto.proyecto_techlab_springboot.exception.sale;

public class MinimumWholesaleQuantityException extends RuntimeException {
    public MinimumWholesaleQuantityException(String message) {
        super(message);
    }

    public MinimumWholesaleQuantityException(int totalItems, int minimumRequired) {
        super("Los pedidos mayoristas requieren un mínimo de %d unidades. Cantidad actual: %d"
                .formatted(minimumRequired, totalItems)
        );
    }
}