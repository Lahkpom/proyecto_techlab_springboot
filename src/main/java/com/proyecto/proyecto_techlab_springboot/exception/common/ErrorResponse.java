package com.proyecto.proyecto_techlab_springboot.exception.common;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int     status,
        String  error,
        String  detail,
        String  path
) {
    public static ErrorResponse of(
            int     status,
            String  error,
            String  detail,
            String  path
    ) {
        return new ErrorResponse(
                Instant.now(),
                status,
                error,
                detail,
                path
        );
    }
}