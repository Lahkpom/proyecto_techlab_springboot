package com.proyecto.proyecto_techlab_springboot.auth.exception;

public class ExpiredPasswordException extends RuntimeException {
    public ExpiredPasswordException() {
        super("La contraseña expiró. Por favor, restablezca su clave.");
    }
}
