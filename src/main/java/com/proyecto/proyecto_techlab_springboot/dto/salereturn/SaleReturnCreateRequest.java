package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnResolution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaleReturnCreateRequest(
        @NotNull(message = "El motivo de la devolución es obligatorio")
        ReturnReason returnReason,

        @NotNull(message = "La resolución pretendida es obligatoria")
        ReturnResolution returnResolution,

        @NotBlank(message = "Debe ingresar una explicación detallada del reclamo")
        @Size(max = 1000, message = "La nota explicativa no puede superar los 1000 caracteres")
        String customerNotes,

        @NotEmpty(message = "Debe seleccionar al menos un producto para la devolución")
        @Valid
        List<SaleReturnItemRequest> items
) {}