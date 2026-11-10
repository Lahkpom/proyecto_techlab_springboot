package com.proyecto.proyecto_techlab_springboot.common.exception.auth;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("El Email aún no ha sido verificado.");
    }
}