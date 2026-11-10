package com.proyecto.proyecto_techlab_springboot.product.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNoLongerAvailableException;

public class ProductNoLongerAvailableException extends ResourceNoLongerAvailableException {
    public ProductNoLongerAvailableException(String params) {
        super("producto", params);
    }
}
