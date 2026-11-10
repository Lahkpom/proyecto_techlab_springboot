package com.proyecto.proyecto_techlab_springboot.sale.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

/**
 * Cuando Mercado Pago notifica un ID de pago inexistente o inalcanzable.
 */
public class PaymentNotFoundException extends ResourceNotFoundException {
    public PaymentNotFoundException(String params) {
        super("pago", params);
    }
}
