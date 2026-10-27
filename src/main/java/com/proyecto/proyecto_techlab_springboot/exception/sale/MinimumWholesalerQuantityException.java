package com.proyecto.proyecto_techlab_springboot.exception.sale;

public class MinimumWholesalerQuantityException extends RuntimeException {
    public MinimumWholesalerQuantityException(String message) {
        super(message);
    }

    public MinimumWholesalerQuantityException(int totalItems, int minimumRequired) {
        super("Los pedidos mayoristas requieren un mínimo de %d unidades. Cantidad actual: %d"
                .formatted(minimumRequired, totalItems)
        );
    }
}