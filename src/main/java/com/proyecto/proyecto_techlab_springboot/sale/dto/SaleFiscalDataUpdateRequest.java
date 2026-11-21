package com.proyecto.proyecto_techlab_springboot.sale.dto;

import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaleFiscalDataUpdateRequest(
        @NotBlank(message = "El nombre o razón social del receptor es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String customerName,

        TaxIdType customerTaxIdType,

        @Size(max = 20, message = "El número de documento fiscal no puede superar los 20 caracteres")
        String customerTaxIdNumber
) {}
