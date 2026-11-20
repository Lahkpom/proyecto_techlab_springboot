package com.proyecto.proyecto_techlab_springboot.image.controller;

import com.proyecto.proyecto_techlab_springboot.image.dto.ImageCreateRequest;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImagePresignedUrlRequest;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImagePresignedUrlResponse;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImageResponse;
import com.proyecto.proyecto_techlab_springboot.image.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products/{productId}/colors/{colorId}/images")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('OPERATOR', 'ADMIN')")
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/presigned-url")
    public ResponseEntity<ImagePresignedUrlResponse> generatePresignedUploadUrl(
            @PathVariable UUID productId,
            @PathVariable Long colorId,
            @Valid @RequestBody ImagePresignedUrlRequest request
    ) {
        return ResponseEntity.ok(imageService.generatePresignedUrl(productId, colorId, request));
    }

    @PostMapping
    public ResponseEntity<ImageResponse> addImage(
            @PathVariable UUID productId,
            @PathVariable Long colorId,
            @Valid @RequestBody ImageCreateRequest request
    ) {
        ImageResponse response = imageService.addImage(productId, colorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ImageResponse>> getImages(
            @PathVariable UUID productId,
            @PathVariable Long colorId
    ) {
        return ResponseEntity.ok(imageService.getImages(productId, colorId));
    }

    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<ImageResponse> setPrimaryImage(
            @PathVariable UUID productId,
            @PathVariable Long colorId,
            @PathVariable Long imageId
    ) {
        return ResponseEntity.ok(imageService.setPrimaryImage(productId, colorId, imageId));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID productId,
            @PathVariable Long colorId,
            @PathVariable Long imageId
    ) {
        imageService.deleteImage(productId, colorId, imageId);
        return ResponseEntity.noContent().build();
    }
}
