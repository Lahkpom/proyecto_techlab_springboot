package com.proyecto.proyecto_techlab_springboot.dto.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Qué tipo de archivo va a subir (para validar extensión/MIME antes de firmar) y para qué variante.
 *
 * @param originalFileName
 * @param contentType
 */
public record ImagePresignedUrlRequest(
        @NotBlank(message = "El nombre del archivo original es obligatorio.")
        String originalFileName,

        @NotBlank(message = "El tipo de contenido (MIME type) es obligatorio.")
        @Pattern(
                regexp = "^(image/jpeg|image/png|image/webp)$",
                message = "Formato no permitido. Solo se aceptan JPEG, PNG y WebP."
        )
        String contentType
) {}