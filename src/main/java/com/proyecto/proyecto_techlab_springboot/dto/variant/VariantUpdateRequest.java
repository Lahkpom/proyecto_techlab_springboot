package com.proyecto.proyecto_techlab_springboot.dto.variant;

import com.proyecto.proyecto_techlab_springboot.model.enums.NumericSize;
import com.proyecto.proyecto_techlab_springboot.model.enums.TargetGender;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record VariantUpdateRequest(
        @NotNull(message = "El id del color es obligatorio")
        Integer colorId,

        @NotNull(message = "El talle es obligatorio")
        NumericSize size,

        @NotNull(message = "El género objetivo es obligatorio")
        TargetGender targetGender,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String desc,

        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 100, message = "El SKU no puede superar los 100 caracteres")
        String sku,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal price,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock
) {}