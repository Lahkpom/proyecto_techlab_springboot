package com.proyecto.proyecto_techlab_springboot.variant.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNoLongerAvailableException;

public class VariantNoLongerAvailableException extends ResourceNoLongerAvailableException {
    public VariantNoLongerAvailableException(String params) {
        super("variante", params);
    }
}
