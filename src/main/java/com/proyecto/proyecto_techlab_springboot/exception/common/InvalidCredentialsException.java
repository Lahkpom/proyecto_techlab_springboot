package com.proyecto.proyecto_techlab_springboot.exception.common;

//import org.springframework.security.authentication.BadCredentialsException;

public class InvalidCredentialsException extends /*BadCredentialsException*/ RuntimeException {
    public InvalidCredentialsException(String param) {
        super("Credenciales inválidas: " + param);
    }
}