package com.proyecto.proyecto_techlab_springboot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SupabaseStorageService {

    private final RestClient restClient;
    private final String bucketName;
    private final String supabaseUrl;

    public SupabaseStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.storage.bucket:products}") String bucketName
    ) {
        this.supabaseUrl = supabaseUrl;
        this.bucketName = bucketName;
        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .defaultHeader("apikey", serviceRoleKey)
                .build();
    }

    /**
     * Solicita una presigned upload URL a Supabase Storage con validez de 10 minutos (600s).
     */
    @SuppressWarnings("unchecked")
    public String createSignedUploadUrl(String storagePath) {
        String endpoint = String.format("/object/upload/sign/%s/%s", bucketName, storagePath);

        Map<String, Object> response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("expiresIn", 600))
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("url")) {
            throw new IllegalStateException("No se pudo obtener la presigned URL de Supabase Storage.");
        }

        // Supabase devuelve una ruta relativa /object/upload/sign/...
        return supabaseUrl + "/storage/v1" + response.get("url");
    }

    /**
     * Resuelve la URL pública para lectura de la imagen alojada.
     */
    public String buildPublicUrl(String storagePath) {
        return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, storagePath);
    }

    /**
     * Elimina el archivo binario del bucket de Supabase.
     */
    public void deleteFile(String storagePath) {
        try {
            restClient.delete()
                    .uri(String.format("/object/%s", bucketName))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(storagePath)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Archivo eliminado de Supabase Storage: {}", storagePath);
        } catch (Exception ex) {
            log.error("Error al eliminar archivo '{}' en Supabase Storage: {}", storagePath, ex.getMessage());
            // No cortamos la transacción de DB si el storage falla al limpiar archivos huérfanos
        }
    }

    /**
     * Helper para extraer el storagePath a partir de la URL pública.
     */
    public String extractPathFromPublicUrl(String publicUrl) {
        String prefix = String.format("/storage/v1/object/public/%s/", bucketName);
        int index = publicUrl.indexOf(prefix);
        if (index != -1) {
            return publicUrl.substring(index + prefix.length());
        }
        return publicUrl;
    }
}