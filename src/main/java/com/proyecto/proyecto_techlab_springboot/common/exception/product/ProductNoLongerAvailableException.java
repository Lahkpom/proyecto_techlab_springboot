package com.proyecto.proyecto_techlab_springboot.common.exception.product;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNoLongerAvailableException;

public class ProductNoLongerAvailableException extends ResourceNoLongerAvailableException {
    public ProductNoLongerAvailableException(String params) {
        super("producto", params);
    }
}
