package com.proyecto.proyecto_techlab_springboot.common.exception.common;

import static com.proyecto.proyecto_techlab_springboot.common.util.AuxiliarFunction.capitalize;

public abstract class ResourceAlreadyExistsException extends RuntimeException {
    protected ResourceAlreadyExistsException(String obj, String params) {
        super("Ya existe un/a %s con las credenciales: %s".formatted(capitalize(obj), params));
    }
}