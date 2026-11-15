package com.proyecto.proyecto_techlab_springboot.image.service;

import com.proyecto.proyecto_techlab_springboot.color.model.Color;
import com.proyecto.proyecto_techlab_springboot.color.service.ColorService;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.image.dto.*;
import com.proyecto.proyecto_techlab_springboot.image.exception.ImageCollectionNotAvailableException;
import com.proyecto.proyecto_techlab_springboot.image.exception.ImageNotFoundException;
import com.proyecto.proyecto_techlab_springboot.image.exception.InvalidImageReorderException;
import com.proyecto.proyecto_techlab_springboot.image.model.Image;
import com.proyecto.proyecto_techlab_springboot.image.repository.IImageRepository;
import com.proyecto.proyecto_techlab_springboot.image.repository.ImagePrimaryProjection;
import com.proyecto.proyecto_techlab_springboot.product.event.ProductDeletedEvent;
import com.proyecto.proyecto_techlab_springboot.product.model.Product;
import com.proyecto.proyecto_techlab_springboot.product.service.ProductService;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final IImageRepository imageRepository;
    private final ProductService productService;
    private final ColorService colorService;
    private final VariantService variantService;
    private final SupabaseStorageService storageService;

    public ImagePresignedUrlResponse generatePresignedUrl(
            UUID productId,
            Long colorId,
            ImagePresignedUrlRequest request
    ) {
        requireAvailableGallery(productId, colorId);

        String extension = extractExtension(request.originalFileName(), request.contentType());
        String storagePath = "products/%s/colors/%d/%s.%s".formatted(
                productId,
                colorId,
                UUID.randomUUID(),
                extension
        );

        String uploadUrl = this.storageService.createSignedUploadUrl(storagePath);
        String publicUrl = this.storageService.buildPublicUrl(storagePath);
        return new ImagePresignedUrlResponse(uploadUrl, publicUrl, storagePath);
    }

    @Transactional
    public ImageResponse addImage(UUID productId, Long colorId, ImageCreateRequest request) {
        Gallery gallery = requireAvailableGallery(productId, colorId);
        int nextOrder = this.imageRepository.findMaxDisplayOrderByProductIdAndColorId(productId, colorId) + 1;
        boolean shouldBePrimary = request.isPrimary() || nextOrder == 0;

        if (shouldBePrimary)
            this.imageRepository.resetPrimaryByProductIdAndColorId(productId, colorId);

        Image image = Image.builder()
                .product(gallery.product())
                .color(gallery.color())
                .imageUrl(request.imageUrl())
                .displayOrder(nextOrder)
                .isPrimary(shouldBePrimary)
                .build();

        return mapToResponse(this.imageRepository.saveAndFlush(image));
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(UUID productId, Long colorId) {
        requireKnownGallery(productId, colorId);
        return this.imageRepository.findAllByProductIdAndColorIdOrdered(productId, colorId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ImageResponse setPrimaryImage(UUID productId, Long colorId, Long imageId) {
        requireKnownGallery(productId, colorId);
        Image image = findByGalleryAndIdOrThrow(productId, colorId, imageId);

        if (!image.isPrimary()) {
            this.imageRepository.resetPrimaryByProductIdAndColorId(productId, colorId);
            image.setPrimary(true);
            this.imageRepository.flush();
        }

        return mapToResponse(image);
    }

    @Transactional
    public List<ImageResponse> reorderImages(UUID productId, Long colorId, ImageReorderRequest request) {
        requireKnownGallery(productId, colorId);

        List<Long> incomingIds = request.orderedImageIds();
        Set<Long> uniqueIds = new HashSet<>(incomingIds);
        if (uniqueIds.size() != incomingIds.size())
            throw new InvalidImageReorderException("La lista de reordenamiento contiene identificadores duplicados.");

        List<Image> existingImages = this.imageRepository.findAllByProductIdAndColorIdOrdered(productId, colorId);
        Set<Long> existingIds = existingImages.stream()
                .map(Image::getImageId)
                .collect(Collectors.toSet());
        if (!existingIds.equals(uniqueIds))
            throw new InvalidImageReorderException(
                    "Los IDs provistos no coinciden exactamente con las imágenes existentes del producto y color."
            );

        Map<Long, Image> imageMap = existingImages.stream()
                .collect(Collectors.toMap(Image::getImageId, Function.identity()));
        for (int i = 0; i < incomingIds.size(); i++)
            imageMap.get(incomingIds.get(i)).setDisplayOrder(i);

        return existingImages.stream()
                .sorted(Comparator.comparing(Image::isPrimary).reversed().thenComparing(Image::getDisplayOrder))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void deleteImage(UUID productId, Long colorId, Long imageId) {
        Image image = findByGalleryAndIdOrThrow(productId, colorId, imageId);
        String storagePath = this.storageService.extractPathFromPublicUrl(image.getImageUrl());

        this.storageService.deleteFiles(List.of(storagePath));
        this.imageRepository.delete(image);

        if (image.isPrimary()) {
            List<Image> remaining = this.imageRepository.findAllByProductIdAndColorIdOrdered(productId, colorId);
            if (!remaining.isEmpty()) {
                remaining.getFirst().setPrimary(true);
                this.imageRepository.flush();
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<ImageKey, String> getPrimaryImageUrls(Collection<ImageKey> requestedKeys) {
        if (requestedKeys.isEmpty())
            return Map.of();

        String imageKeys = requestedKeys.stream()
                .distinct()
                .map(key -> "{\"product_id\":\"%s\",\"color_id\":%d}".formatted(
                        key.productId(),
                        key.colorId()
                ))
                .collect(Collectors.joining(",", "[", "]"));
        List<ImagePrimaryProjection> projections = this.imageRepository.findPrimaryImagesByPairs(imageKeys);

        Map<ImageKey, String> urls = new HashMap<>();
        for (ImagePrimaryProjection projection : projections)
            urls.put(new ImageKey(projection.getProductId(), projection.getColorId()), projection.getImageUrl());
        return urls;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional
    public void deleteProductImages(ProductDeletedEvent event) {
        List<Image> images = this.imageRepository.findAllByProductIdForDeletion(event.productId());
        if (images.isEmpty())
            return;

        List<String> storagePaths = images.stream()
                .map(Image::getImageUrl)
                .map(this.storageService::extractPathFromPublicUrl)
                .toList();
        this.storageService.deleteFiles(storagePaths);
        this.imageRepository.deleteAll(images);
        this.imageRepository.flush();
        log.info("Se eliminaron {} imágenes del producto {}", images.size(), event.productId());
    }

    private Gallery requireAvailableGallery(UUID productId, Long colorId) {
        Gallery gallery = requireKnownGallery(productId, colorId);
        if (gallery.product().getRowStatus() != RowStatus.ACTIVE
                || gallery.color().getRowStatus() != RowStatus.ACTIVE
                || !this.variantService.hasActiveVariant(productId, colorId))
            throw new ImageCollectionNotAvailableException(
                    "{ productId: %s, colorId: %d }".formatted(productId, colorId)
            );
        return gallery;
    }

    private Gallery requireKnownGallery(UUID productId, Long colorId) {
        Product product = this.productService.findProductByIdOrThrow(productId);
        Color color = this.colorService.findColorByIdOrThrow(colorId);
        return new Gallery(product, color);
    }

    private Image findByGalleryAndIdOrThrow(UUID productId, Long colorId, Long imageId) {
        return this.imageRepository.findByImageIdAndProduct_ProductIdAndColor_ColorId(imageId, productId, colorId)
                .orElseThrow(() -> new ImageNotFoundException(
                        "{ imageId: %s, productId: %s, colorId: %s }".formatted(imageId, productId, colorId)
                ));
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains("."))
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private ImageResponse mapToResponse(Image image) {
        return new ImageResponse(
                image.getImageId(),
                image.getProduct().getProductId(),
                image.getColor().getColorId(),
                image.getImageUrl(),
                image.getDisplayOrder(),
                image.isPrimary(),
                image.getCreatedAt(),
                image.getUpdatedAt()
        );
    }

    private record Gallery(Product product, Color color) {}
}
