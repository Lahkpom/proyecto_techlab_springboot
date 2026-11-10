package com.proyecto.proyecto_techlab_springboot.common.exception.variant;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNoLongerAvailableException;

public class VariantNoLongerAvailableException extends ResourceNoLongerAvailableException {
    public VariantNoLongerAvailableException(String params) {
        super("variante", params);
    }
}
