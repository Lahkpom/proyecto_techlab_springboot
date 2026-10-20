package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IImageRepository extends JpaRepository<Image, Long> {

    @Query("""
        SELECT i FROM Image i
        WHERE i.variant.variantId = :variantId
        ORDER BY i.isPrimary DESC, i.displayOrder ASC, i.createdAt ASC
    """)
    List<Image> findAllByVariantIdOrdered(@Param("variantId") Long variantId);

    @Query("""
        SELECT i FROM Image i
        WHERE i.variant.variantId = :variantId AND i.isPrimary = true
    """)
    Optional<Image> findPrimaryByVariantId(@Param("variantId") Long variantId);

    @Query("""
        SELECT COALESCE(MAX(i.displayOrder), -1)
        FROM Image i
        WHERE i.variant.variantId = :variantId
    """)
    int findMaxDisplayOrderByVariantId(@Param("variantId") Long variantId);

    @Query("""
        SELECT i.imageId FROM Image i
        WHERE i.variant.variantId = :variantId
    """)
    List<Long> findImageIdsByVariantId(@Param("variantId") Long variantId);

    Optional<Image> findByImageIdAndVariant_VariantId(Long imageId, Long variantId);

    @Modifying
    @Query("""
        UPDATE Image i
        SET i.isPrimary = false
        WHERE i.variant.variantId = :variantId AND i.isPrimary = true
    """)
    void resetPrimaryByVariantId(@Param("variantId") Long variantId);
}