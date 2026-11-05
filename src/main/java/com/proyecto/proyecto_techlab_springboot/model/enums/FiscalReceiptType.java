package com.proyecto.proyecto_techlab_springboot.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FiscalReceiptType {
    FACTURA_C       (11, "Factura C"),
    NOTA_DEBITO_C   (12, "Nota de Débito C"),
    NOTA_CREDITO_C  (13, "Nota de Crédito C");

    private final int       afipCode;
    private final String    description;
}