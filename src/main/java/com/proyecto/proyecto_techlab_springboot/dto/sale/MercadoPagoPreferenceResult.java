package com.proyecto.proyecto_techlab_springboot.dto.sale;

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