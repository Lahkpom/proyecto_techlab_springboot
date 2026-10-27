package com.proyecto.proyecto_techlab_springboot.dto.sale;

import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload de entrada para iniciar la orden de compra. La lista items es opcional si el usuario está autenticado y tiene carrito en base de datos, pero 
 * obligatoria para compras anónimas.
 */
public record CheckoutRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 255, message = "El nombre no puede exceder los 255 caracteres")
        String customerName,

        @NotBlank(message = "El email del cliente es obligatorio")
        @Email(message = "El formato de email no es válido")
        @Size(max = 255, message = "El email no puede exceder los 255 caracteres")
        String customerEmail,

        @NotBlank(message = "El teléfono de contacto es obligatorio")
        @Size(max = 50, message = "El teléfono no puede exceder los 50 caracteres")
        String customerPhone,

        @NotBlank(message = "La dirección de entrega es obligatoria")
        String shippingAddress,

        String customerNotes,

        BigDecimal shippingCost,

        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod paymentMethod,

        @Valid
        List<CheckoutItemRequest> items
) {}