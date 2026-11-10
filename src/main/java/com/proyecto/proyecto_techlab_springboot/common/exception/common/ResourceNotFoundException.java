package com.proyecto.proyecto_techlab_springboot.common.exception.common;

import static com.proyecto.proyecto_techlab_springboot.common.util.AuxiliarFunction.capitalize;

public abstract class ResourceNotFoundException extends RuntimeException {
    protected ResourceNotFoundException(String obj, String params) {
        super("%s no existe o los parámetros de búsqueda son inválidos: %s".formatted(capitalize(obj), params));
    }
}