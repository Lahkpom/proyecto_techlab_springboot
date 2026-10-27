package com.proyecto.proyecto_techlab_springboot.dto.sale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para que los operadores agreguen o editen notas internas del pedido sin alterar el ciclo de vida comercial.
 */
public record SaleInternalNoteRequest(
        @NotBlank(message = "La nota interna no puede estar vacía")
        @Size(max = 2000, message = "La nota no puede superar los 2000 caracteres")
        String internalNotes
) {}