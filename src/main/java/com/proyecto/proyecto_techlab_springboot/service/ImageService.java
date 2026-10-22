package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.image.ImageCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImagePresignedUrlRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImagePresignedUrlResponse;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImageReorderRequest;
import com.proyecto.proyecto_techlab_springboot.dto.image.ImageResponse;
import com.proyecto.proyecto_techlab_springboot.exception.image.ImageNotFoundException;
import com.proyecto.proyecto_techlab_springboot.exception.image.InvalidImageReorderException;
import com.proyecto.proyecto_techlab_springboot.model.Image;
import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.repository.IImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final IImageRepository          iImageRepository;
    private final VariantService            variantService;
    private final SupabaseStorageService    storageService;

    @Transactional(readOnly = true)
    public ImagePresignedUrlResponse generatePresignedUrl(Long variantId, ImagePresignedUrlRequest request) {
        this.variantService.findVariantByIdOrThrow(variantId);

        String extension    = extractExtension(request.originalFileName(), request.contentType());
        String storagePath  = "variants/%d/%s.%s".formatted(variantId, UUID.randomUUID(), extension);

        String uploadUrl = this.storageService.createSignedUploadUrl(storagePath);
        String publicUrl = this.storageService.buildPublicUrl(storagePath);

        return new ImagePresignedUrlResponse(uploadUrl, publicUrl, storagePath);
    }

    @Transactional
    public ImageResponse addImage(Long variantId, ImageCreateRequest request) {
        Variant variant = this.variantService.findVariantByIdOrThrow(variantId);

        this.variantService.requireAvailableVariantOrThrow(variant);

        int     nextOrder       = this.iImageRepository.findMaxDisplayOrderByVariantId(variantId) + 1;
        boolean shouldBePrimary = request.isPrimary();

        // Si es la primera imagen de la variante, obligatoriamente es la principal
        if (nextOrder == 0) {
            shouldBePrimary = true;
        } else if (shouldBePrimary) {
            this.iImageRepository.resetPrimaryByVariantId(variantId);
        }

        Image image = Image.builder()
                .variant(variant)
                .imageUrl(request.imageUrl())
                .displayOrder(nextOrder)
                .isPrimary(shouldBePrimary)
                .build();

        Image saved = this.iImageRepository.save(image);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getImagesByVariant(Long variantId) {
        this.variantService.findVariantByIdOrThrow(variantId);

        return this.iImageRepository.findAllByVariantIdOrdered(variantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ImageResponse setPrimaryImage(Long variantId, Long imageId) {
        this.variantService.findVariantByIdOrThrow(variantId);

        Image image = this.findImageByVariantIdAndImageIdOrThrow(variantId, imageId);

        if (!image.isPrimary()) {
            this.iImageRepository.resetPrimaryByVariantId(variantId);
            image.setPrimary(true);
        }

        return mapToResponse(image);
    }

    @Transactional
    public List<ImageResponse> reorderImages(Long variantId, ImageReorderRequest request) {
        this.variantService.findVariantByIdOrThrow(variantId);

        List<Long>  incomingIds = request.orderedImageIds();
        Set<Long>   uniqueIds   = new HashSet<>(incomingIds);

        if (uniqueIds.size() != incomingIds.size())
            throw new InvalidImageReorderException("La lista de reordenamiento contiene identificadores duplicados.");

        List<Image> existingImages  = this.iImageRepository.findAllByVariantIdOrdered(variantId);
        Set<Long>   existingIds     = existingImages.stream().map(Image::getImageId).collect(Collectors.toSet());

        if (!existingIds.equals(uniqueIds))
            throw new InvalidImageReorderException("Los IDs provistos no coinciden exactamente con las imágenes existentes de la variante.");

        Map<Long, Image> imageMap = existingImages.stream()
                .collect(Collectors.toMap(Image::getImageId, Function.identity()));

        // Aplicamos el nuevo índice a cada entidad en memoria (dirty checking dispara los UPDATEs)
        for (int i = 0; i < incomingIds.size(); i++) {
            Long    imageId = incomingIds.get(i);
            Image   image   = imageMap.get(imageId);
            image.setDisplayOrder(i);
        }

        return existingImages.stream()
                .sorted((a, b) -> {
                    if (a.isPrimary() != b.isPrimary())
                        return a.isPrimary() ? -1 : 1;

                    return Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                })
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void deleteImage(Long variantId, Long imageId) {
        this.variantService.findVariantByIdOrThrow(variantId);

        Image image = this.findImageByVariantIdAndImageIdOrThrow(variantId, imageId);

        String storagePath = this.storageService.extractPathFromPublicUrl(image.getImageUrl());

        // Borrado físico en base de datos
        this.iImageRepository.delete(image);

        // Si eliminamos la portada y quedan otras imágenes, promovemos la siguiente en orden
        if (image.isPrimary()) {
            List<Image> remaining = this.iImageRepository.findAllByVariantIdOrdered(variantId);
            if (!remaining.isEmpty()) remaining.getFirst().setPrimary(true);
        }

        // Borrado físico del archivo binario en el bucket de Supabase
        this.storageService.deleteFile(storagePath);
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains("."))
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return switch (contentType) {
            case "image/png"    -> "png";
            case "image/webp"   -> "webp";
            default             -> "jpg";
        };
    }

    /**
     * Para evitar que en un llamado se haga un llamado por error a un imageId que corresponde a otra variante,
     * se solicita que se ingrese ambos id para confirmar identidad
     *
     * @param variantId variantId
     * @param imageId   imageId
     * @return Image filtrada por imageId y variantId
     */
    private Image findImageByVariantIdAndImageIdOrThrow(Long variantId, Long imageId){
        return this.iImageRepository.findByImageIdAndVariant_VariantId(imageId, variantId)
                .orElseThrow(() -> new ImageNotFoundException("{ imageId: %s, variantId: %s }".formatted(imageId, variantId)));
    }

    private ImageResponse mapToResponse(Image image) {
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