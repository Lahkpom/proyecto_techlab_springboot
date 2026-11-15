package com.proyecto.proyecto_techlab_springboot.image.repository;

import com.proyecto.proyecto_techlab_springboot.image.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IImageRepository extends JpaRepository<Image, Long> {

    @Query("""
        SELECT i
        FROM Image i
        WHERE i.product.productId = :productId
          AND i.color.colorId = :colorId
        ORDER BY i.isPrimary DESC, i.createdAt ASC
    """)
    List<Image> findGalleryImagesByProductIdAndColorId(
            @Param("productId") UUID productId,
            @Param("colorId") Long colorId
    );

    boolean existsByProduct_ProductIdAndColor_ColorId(UUID productId, Long colorId);

    Optional<Image> findByImageIdAndProduct_ProductIdAndColor_ColorId(
            Long imageId,
            UUID productId,
            Long colorId
    );

    @Query(value = "SELECT * FROM image WHERE product_id = :productId", nativeQuery = true)
    List<Image> findAllByProductIdForDeletion(@Param("productId") UUID productId);

    @Query(value = """
        SELECT i.product_id AS "productId",
               i.color_id AS "colorId",
               i.image_url AS "imageUrl"
        FROM image i
        JOIN jsonb_to_recordset(CAST(:imageKeys AS jsonb))
             AS requested_pair(product_id UUID, color_id BIGINT)
          ON requested_pair.product_id = i.product_id
         AND requested_pair.color_id = i.color_id
        WHERE i.is_primary = TRUE
        """, nativeQuery = true)
    List<ImagePrimaryProjection> findPrimaryImagesByPairs(@Param("imageKeys") String imageKeys);

    @Modifying
    @Query("""
        UPDATE Image i
        SET i.isPrimary = false
        WHERE i.product.productId = :productId
          AND i.color.colorId = :colorId
          AND i.isPrimary = true
    """)
    int resetPrimaryByProductIdAndColorId(
            @Param("productId") UUID productId,
            @Param("colorId") Long colorId
    );
}
