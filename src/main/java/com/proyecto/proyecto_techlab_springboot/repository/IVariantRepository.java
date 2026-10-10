package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.model.enums.NumericSize;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IVariantRepository extends JpaRepository<Variant, UUID> {

    // Validar combinación producto + color + talle duplicada
    boolean existsByProduct_ProductIdAndColor_ColorIdAndVariantSize(
            UUID        productId,
            Integer     colorId,
            NumericSize variantSize
    );
    boolean existsByProduct_ProductIdAndColor_ColorIdAndVariantSizeAndVariantIdNot(
            UUID        productId,
            Integer     colorId,
            NumericSize variantSize,
            UUID        variantId
    );

    // Validar SKU duplicado
    boolean existsByVariantSkuIgnoreCase(String sku);
    // Para el update: existe con ese atributo PERO con un ID distinto al que estoy editando
    boolean existsByVariantSkuIgnoreCaseAndVariantIdNot(String sku, UUID variantId);

    // Buscar todas las variantes de un producto
    List<Variant> findAllByProduct_ProductId(UUID productId);

    // Buscar variantes activas de un producto
    List<Variant> findAllByProduct_ProductIdAndRowStatus(UUID productId, RowStatus rowStatus);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE
            Variant v
        SET
            v.rowStatus = :newStatus
        WHERE
            v.product.productId = :productId
            AND v.rowStatus     = :currentStatus
    """)
    void updateStatusByProductId(
            @Param("productId")     UUID productId,
            @Param("newStatus")     RowStatus newStatus,
            @Param("currentStatus") RowStatus currentStatus
    );
}