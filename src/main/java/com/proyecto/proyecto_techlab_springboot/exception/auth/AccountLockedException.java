package com.proyecto.proyecto_techlab_springboot.exception.auth;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String time) {
        super("La cuenta está temporalmente suspendida por reiterados intentos fallidos hasta %s".formatted(time));
    }
}