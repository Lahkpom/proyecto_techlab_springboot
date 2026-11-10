package com.proyecto.proyecto_techlab_springboot.fiscal.exception;

/**
 * ARCA rechaza la emisión de la factura por problemas con los datos fiscales
 */
public class ArcaBusinessRejectionException extends RuntimeException {
    public ArcaBusinessRejectionException(String message) {
        super(message);
    }
}