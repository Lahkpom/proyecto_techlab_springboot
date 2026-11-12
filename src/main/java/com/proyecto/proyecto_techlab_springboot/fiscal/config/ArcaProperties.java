package com.proyecto.proyecto_techlab_springboot.fiscal.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Incluye todas las propiedades bajo el prefijo arca.* del application.properties con validaciones de arranque (@Validated) para frenar la aplicación si falta algún dato en producción:
 * @param cuit
 * @param puntoVenta
 * @param certPath
 * @param keyPath
 * @param wsaaUrl
 * @param wsfeUrl
 * @param anonymousInvoiceLimit
 */
@Validated
@ConfigurationProperties(prefix = "arca")
public record ArcaProperties(
        @NotBlank 
        String cuit,
        
        @NotNull  
        @Positive 
        Integer puntoVenta,
        
        @NotBlank 
        String certPath,
        
        @NotBlank 
        String keyPath,
        
        @NotBlank 
        String wsaaUrl,
        
        @NotBlank 
        String wsfeUrl,
        
        @NotNull  
        @Positive 
        BigDecimal anonymousInvoiceLimit
) {}