package com.proyecto.proyecto_techlab_springboot.common.exception.common;

import static com.proyecto.proyecto_techlab_springboot.common.util.AuxiliarFunction.capitalize;

public abstract class ResourceNoLongerAvailableException extends RuntimeException {
    protected ResourceNoLongerAvailableException(String obj, String params) {
        super("%s ya no se encuentra disponible: %s".formatted(capitalize(obj), params));
    }
}