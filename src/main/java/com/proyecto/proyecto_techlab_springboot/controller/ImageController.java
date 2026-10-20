package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.image.ImageCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImagePresignedUrlRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImagePresignedUrlResponse;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImageReorderRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImageResponse;
import com.proyecto.proyecto_techlab_springboot.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/variants/{variantId}/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /**
     * Paso 1 del flujo de subida:
     * Genera la Presigned URL de Supabase para que el cliente suba el binario directo al storage.
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<ImagePresignedUrlResponse> generatePresignedUploadUrl(
            @PathVariable Long variantId,
            @Valid @RequestBody ImagePresignedUrlRequest request
    ) {
        return ResponseEntity.ok(imageService.generatePresignedUrl(variantId, request));
    }

    /**
     * Paso 3 del flujo de subida:
     * Trasladar el éxito de la subida a la base de datos (persistencia física).
     */
    @PostMapping
    public ResponseEntity<ImageResponse> addImageToVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody ImageCreateRequest request
    ) {
        ImageResponse response = imageService.addImage(variantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Consulta la galería completa de la variante ordenada (is_primary DESC, display_order ASC).
     */
    @GetMapping
    public ResponseEntity<List<ImageResponse>> getImagesByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(imageService.getImagesByVariant(variantId));
    }

    /**
     * Designa una imagen específica como la portada (is_primary = true) y desmarca la anterior.
     * No requiere body: los identificadores viajan en la ruta.
     */
    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<ImageResponse> setPrimaryImage(
            @PathVariable Long variantId,
            @PathVariable Long imageId
    ) {
        return ResponseEntity.ok(imageService.setPrimaryImage(variantId, imageId));
    }

    /**
     * Sincroniza el nuevo orden de la galería (drag & drop en frontend)
     * mediante la lista completa de identificadores.
     */
    @PatchMapping("/reorder")
    public ResponseEntity<List<ImageResponse>> reorderImages(
            @PathVariable Long variantId,
            @Valid @RequestBody ImageReorderRequest request
    ) {
        return ResponseEntity.ok(imageService.reorderImages(variantId, request));
    }

    /**
     * Borrado físico: elimina el registro de PostgreSQL y el archivo en Supabase Storage.
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long variantId,
            @PathVariable Long imageId
    ) {
        imageService.deleteImage(variantId, imageId);
        return ResponseEntity.noContent().build();
    }
}