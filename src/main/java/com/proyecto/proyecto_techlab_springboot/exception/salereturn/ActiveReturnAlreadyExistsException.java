package com.proyecto.proyecto_techlab_springboot.exception.salereturn;

import java.util.UUID;

public class ActiveReturnAlreadyExistsException extends RuntimeException {
    public ActiveReturnAlreadyExistsException(UUID saleId) {
        super("La orden { ID: %s } ya cuenta con un expediente de devolución activo en trámite.".formatted(saleId));
    }
}