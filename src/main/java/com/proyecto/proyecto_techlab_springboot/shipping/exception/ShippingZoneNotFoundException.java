package com.proyecto.proyecto_techlab_springboot.shipping.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class ShippingZoneNotFoundException extends ResourceNotFoundException {
    public ShippingZoneNotFoundException(String params) {
        super("zona de envío", params);
    }
}
