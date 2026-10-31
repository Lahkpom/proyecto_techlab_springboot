package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaleReturnStatusUpdateRequest(
        @NotNull(message = "El nuevo estado de la devolución es obligatorio")
        ReturnStatus newStatus,

        @Size(max = 100, message = "El número de seguimiento no puede exceder los 100 caracteres")
        String returnShippingGuide,

        @Size(max = 500, message = "La nota de cambio de estado no puede exceder los 500 caracteres")
        String statusNotes
) {}