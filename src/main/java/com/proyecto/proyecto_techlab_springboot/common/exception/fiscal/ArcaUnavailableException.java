package com.proyecto.proyecto_techlab_springboot.common.exception.fiscal;

/**
 * Caídas transitorias de infraestructura de ARCA (Errores 5xx de parte de ellos).
 */
public class ArcaUnavailableException extends RuntimeException {
    public ArcaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}