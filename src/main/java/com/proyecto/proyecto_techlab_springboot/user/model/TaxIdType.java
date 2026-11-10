package com.proyecto.proyecto_techlab_springboot.user.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaxIdType {
    DNI         (96, "Documento Nacional de Identidad"),
    CUIL        (86, "Código Único de Identificación Laboral"),
    CUIT        (80, "Clave Única de Identificación Tributaria"),
    PASAPORTE   (94, "Pasaporte");

    /**
     * Código de tipo de documento estipulado por ARCA / AFIP en el WSFE.
     */
    private final int       afipCode;
    private final String    description;
}