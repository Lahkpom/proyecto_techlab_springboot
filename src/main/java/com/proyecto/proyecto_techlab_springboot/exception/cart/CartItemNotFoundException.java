package com.proyecto.proyecto_techlab_springboot.exception.cart;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;

public class CartItemNotFoundException extends ResourceNotFoundException {
    public CartItemNotFoundException(String params) {
        super("item", "No se encuentra en el carrito el item solicitado asociado al usuario indicado. %s".formatted(params));
    }
}
