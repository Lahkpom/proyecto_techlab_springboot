package com.proyecto.proyecto_techlab_springboot.product.exception;

public class InvalidProductCatalogSortException extends RuntimeException {
    public InvalidProductCatalogSortException(String property) {
        super("El campo '%s' no se puede utilizar para ordenar el catálogo público.".formatted(property));
    }
}
