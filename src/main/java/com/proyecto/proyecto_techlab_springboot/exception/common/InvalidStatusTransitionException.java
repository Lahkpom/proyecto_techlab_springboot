package com.proyecto.proyecto_techlab_springboot.exception.common;

public abstract class InvalidStatusTransitionException extends RuntimeException {

    protected InvalidStatusTransitionException(String entityName, String currentStatus, String targetStatus) {
        super("Transición de estado para la %s no permitida: { %s -> %s}.".formatted(entityName, currentStatus, targetStatus));
    }

    protected InvalidStatusTransitionException(String message) {
        super(message);
    }
}