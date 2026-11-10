package com.proyecto.proyecto_techlab_springboot.user.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceAlreadyExistsException;

public class UserAlreadyExistsException extends ResourceAlreadyExistsException {
    public UserAlreadyExistsException(String params) {
        super("usuario", params);
    }
}
