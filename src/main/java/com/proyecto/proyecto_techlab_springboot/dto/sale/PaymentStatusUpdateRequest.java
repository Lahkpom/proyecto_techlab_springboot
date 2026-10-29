package com.proyecto.proyecto_techlab_springboot.dto.sale;

import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload para que el administrador actualice pagos de transferencias o ajustes manuales
 * 
 * @param newPaymentStatus
 * @param paymentNotes
 */
public record PaymentStatusUpdateRequest(
        @NotNull(message = "El nuevo estado del pago es obligatorio")
        PaymentStatus newPaymentStatus,

        @Size(max = 500, message = "La nota de conciliación no puede exceder los 500 caracteres")
        String paymentNotes
) {}