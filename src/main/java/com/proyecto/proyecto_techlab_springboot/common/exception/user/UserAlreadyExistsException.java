package com.proyecto.proyecto_techlab_springboot.common.exception.user;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceAlreadyExistsException;

public class UserAlreadyExistsException extends ResourceAlreadyExistsException {
    public UserAlreadyExistsException(String params) {
        super("usuario", params);
    }
}
