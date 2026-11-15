package com.proyecto.proyecto_techlab_springboot.image.controller;

import com.proyecto.proyecto_techlab_springboot.image.dto.ImageCreateRequest;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImagePresignedUrlRequest;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImagePresignedUrlResponse;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImageReorderRequest;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImageResponse;
import com.proyecto.proyecto_techlab_springboot.image.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
// @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')") // TODO: Descomentar al habilitar Spring Security
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/products/{productId}/colors/{colorId}/images/presigned-url")
    public ResponseEntity<ImagePresignedUrlResponse> generatePresignedUploadUrl(
            @PathVariable UUID productId,
            @PathVariable Long colorId,
            @Valid @RequestBody ImagePresignedUrlRequest request
    ) {
        return ResponseEntity.ok(imageService.generatePresignedUrl(productId, colorId, request));
    }

    @PostMapping("/products/{productId}/colors/{colorId}/images")
    public ResponseEntity<ImageResponse> addImage(
            @PathVariable UUID productId,
            @PathVariable Long colorId,
            @Valid @RequestBody ImageCreateRequest request
    ) {
        ImageResponse response = imageService.addImage(productId, colorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/products/{productId}/colors/{colorId}/images")
    public ResponseEntity<List<ImageResponse>> getImages(
            @PathVariable UUID productId,
            @PathVariable Long colorId
    ) {
        return ResponseEntity.ok(imageService.getImages(productId, colorId));
    }

    @PatchMapping("/products/{productId}/colors/{colorId}/images/{imageId}/primary")
    public ResponseEntity<ImageResponse> setPrimaryImage(
            @PathVariable UUID productId,
            @PathVariable Long colorId,
            @PathVariable Long imageId
    ) {
        return ResponseEntity.ok(imageService.setPrimaryImage(productId, colorId, imageId));
    }

    @PatchMapping("/products/{productId}/colors/{colorId}/images/reorder")
    public ResponseEntity<List<ImageResponse>> reorderImages(
            @PathVariable UUID productId,
            @PathVariable Long colorId,
            @Valid @RequestBody ImageReorderRequest request
    ) {
        return ResponseEntity.ok(imageService.reorderImages(productId, colorId, request));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }
}
