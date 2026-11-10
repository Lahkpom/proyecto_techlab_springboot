package com.proyecto.proyecto_techlab_springboot.common.exception;

//import org.springframework.security.authentication.BadCredentialsException;

public class InvalidCredentialsException extends /*BadCredentialsException*/ RuntimeException {
    public InvalidCredentialsException(String param) {
        super("Credenciales inválidas: " + param);
    }
}