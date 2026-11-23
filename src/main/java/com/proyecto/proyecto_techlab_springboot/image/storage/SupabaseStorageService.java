package com.proyecto.proyecto_techlab_springboot.image.storage;

import com.proyecto.proyecto_techlab_springboot.image.exception.StorageOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SupabaseStorageService {

    private final RestClient    restClient;
    private final String        bucketName;
    private final String        supabaseUrl;

    public SupabaseStorageService(
            @Value("${supabase.url}")                       String supabaseUrl,
            @Value("${supabase.secret-key}")                String serviceRoleKey,
            @Value("${supabase.storage.bucket:products}")   String bucketName
    ) {
        this.supabaseUrl    = supabaseUrl;
        this.bucketName     = bucketName;
        this.restClient     = RestClient.builder()
                .baseUrl("%s/storage/v1".formatted(this.supabaseUrl))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(serviceRoleKey))
                .defaultHeader("apikey", serviceRoleKey)
                .build();
    }

    /**
     * Solicita una presigned upload URL a Supabase Storage con validez de 10 minutos (600s).
     */
    public String createSignedUploadUrl(String storagePath) {
        String endpoint = "/object/upload/sign/%s/%s".formatted(this.bucketName, storagePath);

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", 600))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            log.error("Error al generar URL firmada en Supabase Storage: {}", ex.getMessage());
            throw new StorageOperationException("No se pudo generar la URL firmada de Supabase Storage.", ex);
        }

        Object signedPath = response != null ? response.get("url") : null;
        if (!(signedPath instanceof String url) || url.isBlank())
            throw new StorageOperationException("No se pudo obtener la presigned URL de Supabase Storage.");

        // Supabase devuelve una ruta relativa /object/upload/sign/...
        return "%s/storage/v1%s".formatted(this.supabaseUrl, url);
    }

    /**
     * Resuelve la URL pública para lectura de la imagen alojada.
     */
    public String buildPublicUrl(String storagePath) {
        return "%s/storage/v1/object/public/%s/%s".formatted(this.supabaseUrl, this.bucketName, storagePath);
    }

    public void deleteFiles(List<String> storagePaths) {
        if (storagePaths.isEmpty())
            return;

        try {
            restClient.method(HttpMethod.DELETE)
                    .uri("/object/%s".formatted(this.bucketName))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", storagePaths))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Se eliminaron {} archivo(s) de Supabase Storage", storagePaths.size());
        } catch (RestClientException ex) {
            log.error("Error al eliminar archivos de Supabase Storage: {}", ex.getMessage());
            throw new StorageOperationException("No se pudieron eliminar los archivos de Supabase Storage.", ex);
        }
    }

    /**
     * Helper para extraer el storagePath a partir de la URL pública.
     */
    public String extractPathFromPublicUrl(String publicUrl) {
        String  prefix  = "/storage/v1/object/public/%s/".formatted(this.bucketName);
        int     index   = publicUrl.indexOf(prefix);

        if (index != -1) return publicUrl.substring(index + prefix.length());

        return publicUrl;
    }
}