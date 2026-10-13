package com.proyecto.proyecto_techlab_springboot.util;

public final class ValidationUtil {
    private ValidationUtil() {}

    public static final String PHONE_REGEX = "^\\d{10}$";

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
