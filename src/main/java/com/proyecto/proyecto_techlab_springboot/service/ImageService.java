package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.image.ImageCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImagePresignedUrlRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImagePresignedUrlResponse;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImageReorderRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImageResponse;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.exception.common.BadRequestException;
import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Image;
import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.repository.IImageRepository;
import com.proyecto.proyecto_techlab_springboot.repository.IVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final IImageRepository imageRepository;
    private final IVariantRepository variantRepository;
    private final SupabaseStorageService storageService;

    @Transactional(readOnly = true)
    public ImagePresignedUrlResponse generatePresignedUrl(Long variantId, ImagePresignedUrlRequest request) {
        validateVariantExists(variantId);

        String extension = extractExtension(request.originalFileName(), request.contentType());
        String storagePath = String.format("variants/%d/%s.%s", variantId, UUID.randomUUID(), extension);

        String uploadUrl = storageService.createSignedUploadUrl(storagePath);
        String publicUrl = storageService.buildPublicUrl(storagePath);

        return new ImagePresignedUrlResponse(uploadUrl, publicUrl, storagePath);
    }

    @Transactional
    public ImageResponse addImage(Long variantId, ImageCreateRequest request) {
        Variant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada con ID: " + variantId));

        if (variant.getRowStatus() != RowStatus.ACTIVE) {
            throw new ResourceNotFoundException("La variante indicada no se encuentra activa.");
        }

        int nextOrder = imageRepository.findMaxDisplayOrderByVariantId(variantId) + 1;
        boolean shouldBePrimary = Boolean.TRUE.equals(request.isPrimary());

        // Si es la primera imagen de la variante, obligatoriamente es la principal
        if (nextOrder == 0) {
            shouldBePrimary = true;
        } else if (shouldBePrimary) {
            imageRepository.resetPrimaryByVariantId(variantId);
        }

        Image image = Image.builder()
                .variant(variant)
                .imageUrl(request.publicUrl())
                .displayOrder(nextOrder)
                .isPrimary(shouldBePrimary)
                .build();

        Image saved = imageRepository.save(image);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getImagesByVariant(Long variantId) {
        validateVariantExists(variantId);
        return imageRepository.findAllByVariantIdOrdered(variantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ImageResponse setPrimaryImage(Long variantId, Long imageId) {
        validateVariantExists(variantId);

        Image targetImage = imageRepository.findByImageIdAndVariant_VariantId(imageId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada para la variante especificada."));

        if (!targetImage.isPrimary()) {
            imageRepository.resetPrimaryByVariantId(variantId);
            targetImage.setPrimary(true);
            // Dirty checking ejecutará el UPDATE al cerrar la transacción
        }

        return toResponse(targetImage);
    }

    @Transactional
    public List<ImageResponse> reorderImages(Long variantId, ImageReorderRequest request) {
        validateVariantExists(variantId);

        List<Long> incomingIds = request.orderedImageIds();
        Set<Long> uniqueIds = new HashSet<>(incomingIds);

        if (uniqueIds.size() != incomingIds.size()) {
            throw new BadRequestException("La lista de reordenamiento contiene identificadores duplicados.");
        }

        List<Image> existingImages = imageRepository.findAllByVariantIdOrdered(variantId);
        Set<Long> existingIds = existingImages.stream().map(Image::getImageId).collect(Collectors.toSet());

        if (!existingIds.equals(uniqueIds)) {
            throw new BadRequestException("Los IDs provistos no coinciden exactamente con las imágenes existentes de la variante.");
        }

        Map<Long, Image> imageMap = existingImages.stream()
                .collect(Collectors.toMap(Image::getImageId, Function.identity()));

        // Aplicamos el nuevo índice a cada entidad en memoria (dirty checking dispara los UPDATEs)
        for (int i = 0; i < incomingIds.size(); i++) {
            Long imageId = incomingIds.get(i);
            Image image = imageMap.get(imageId);
            image.setDisplayOrder(i);
        }

        return existingImages.stream()
                .sorted((a, b) -> {
                    if (a.isPrimary() != b.isPrimary()) {
                        return a.isPrimary() ? -1 : 1;
                    }
                    return Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                })
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteImage(Long variantId, Long imageId) {
        validateVariantExists(variantId);

        Image image = imageRepository.findByImageIdAndVariant_VariantId(imageId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada con ID: " + imageId));

        boolean wasPrimary = image.isPrimary();
        String storagePath = storageService.extractPathFromPublicUrl(image.getImageUrl());

        // 1. Borrado físico en base de datos
        imageRepository.delete(image);

        // 2. Si eliminamos la portada y quedan otras imágenes, promovemos la siguiente en orden
        if (wasPrimary) {
            List<Image> remaining = imageRepository.findAllByVariantIdOrdered(variantId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setPrimary(true);
            }
        }

        // 3. Borrado físico del archivo binario en el bucket de Supabase
        storageService.deleteFile(storagePath);
    }

    private void validateVariantExists(Long variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Variante no encontrada con ID: " + variantId);
        }
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private ImageResponse toResponse(Image image) {
        return new ImageResponse(
                image.getImageId(),
                image.getVariant().getVariantId(),
                image.getImageUrl(),
                image.getDisplayOrder(),
                image.isPrimary(),
                image.getCreatedAt(),
                image.getUpdatedAt()
        );
    }
}