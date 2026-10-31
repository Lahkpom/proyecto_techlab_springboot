package com.proyecto.proyecto_techlab_springboot.exception.salereturn;

public class ReturnItemQuantityExceededException extends RuntimeException {
    public ReturnItemQuantityExceededException(Long saleItemId, int requestedQuantity, int availableQuantity) {
        super(
                "Cantidad solicitada para devolución (%d) excede las unidades elegibles (%d) para el ítem { ID: %d }"
                        .formatted(requestedQuantity, availableQuantity, saleItemId)
        );
    }
}