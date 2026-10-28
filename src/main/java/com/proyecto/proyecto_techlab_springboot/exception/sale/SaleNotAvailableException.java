package com.proyecto.proyecto_techlab_springboot.exception.sale;

public class SaleNotAvailableException extends RuntimeException {
    public SaleNotAvailableException() {
        super("No tiene permisos para ver esta orden o la misma no le pertenece.");
    }
}