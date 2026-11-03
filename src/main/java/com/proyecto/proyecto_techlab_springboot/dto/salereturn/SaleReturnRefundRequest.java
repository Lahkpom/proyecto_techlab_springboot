package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import jakarta.validation.constraints.Size;

public record SaleReturnRefundRequest(
        @Size(max = 100, message = "El código o número de referencia no puede superar los 100 caracteres")
        String transactionReference,

        @Size(max = 500, message = "Las notas de conciliación no pueden superar los 500 caracteres")
        String notes
) {}