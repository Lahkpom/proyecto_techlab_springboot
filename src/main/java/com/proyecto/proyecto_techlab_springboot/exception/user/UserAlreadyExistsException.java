package com.proyecto.proyecto_techlab_springboot.exception.user;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceAlreadyExistsException;

public class UserAlreadyExistsException extends ResourceAlreadyExistsException {
    public UserAlreadyExistsException(String params) {
        super("usuario", params);
    }
}
