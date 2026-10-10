package com.proyecto.proyecto_techlab_springboot.util;

import static com.proyecto.proyecto_techlab_springboot.util.ValidationUtil.isBlank;

public final class AuxiliarFunction {
    private AuxiliarFunction() {}

    public static String capitalize(String str) {
        if (isBlank(str)) return str;

        String trimmed = str.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
}