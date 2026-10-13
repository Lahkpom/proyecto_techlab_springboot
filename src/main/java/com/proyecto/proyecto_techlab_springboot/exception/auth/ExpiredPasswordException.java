package com.proyecto.proyecto_techlab_springboot.exception.auth;

public class ExpiredPasswordException extends RuntimeException {
    public ExpiredPasswordException() {
        super("La contraseña expiró. Por favor, restablezca su clave.");
    }
}
