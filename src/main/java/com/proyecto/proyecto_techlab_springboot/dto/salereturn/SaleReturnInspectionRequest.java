package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaleReturnInspectionRequest(
        @NotBlank(message = "El dictamen general del peritaje es obligatorio")
        @Size(max = 1000, message = "Las notas de inspección no pueden superar los 1000 caracteres")
        String inspectionNotes,

        @Size(max = 500, message = "La ruta de evidencias fotográficas no puede exceder los 500 caracteres")
        String inspectionEvidencePath,

        @NotEmpty(message = "Debe incluir el dictamen de cada una de las prendas recibidas")
        @Valid
        List<SaleReturnItemInspectionRequest> inspectedItems
) {}