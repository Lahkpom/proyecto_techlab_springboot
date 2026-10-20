package com.proyecto.proyecto_techlab_springboot.dto.image;

/**
 * Contiene la URL temporal a la que el frontend debe hacer el PUT del binario y la URL pública final que tendrá el archivo
 *
 * @param uploadUrl     URL temporal para hacer el PUT
 * @param publicUrl     URL pública final del archivo
 * @param storagePath
 */
public record ImagePresignedUrlResponse(
        String uploadUrl,
        String publicUrl,
        String storagePath
) {}