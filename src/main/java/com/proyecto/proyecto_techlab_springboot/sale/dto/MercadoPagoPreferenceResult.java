package com.proyecto.proyecto_techlab_springboot.sale.dto;

/**
 * Resultado inmutable devuelto por el cliente de MP
 * 
 * @param preferenceId
 * @param initPoint
 */
public record MercadoPagoPreferenceResult(
        String preferenceId,
        String initPoint
) {}