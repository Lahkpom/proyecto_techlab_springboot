package com.proyecto.proyecto_techlab_springboot.variant.repository;

import com.proyecto.proyecto_techlab_springboot.variant.model.Variant;
import com.proyecto.proyecto_techlab_springboot.variant.model.NumericSize;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IVariantRepository extends JpaRepository<Variant, Long> {

    // Validar combinación producto + color + talle duplicada
    boolean existsByProduct_ProductIdAndColor_ColorIdAndVariantSize(
            UUID        productId,
            Long        colorId,
            NumericSize variantSize
    );
    boolean existsByProduct_ProductIdAndColor_ColorIdAndVariantSizeAndVariantIdNot(
            UUID        productId,
            Long        colorId,
            NumericSize variantSize,
            Long        variantId
    );

    // Validar SKU duplicado
    boolean existsByVariantSkuIgnoreCase(String sku);
    // Para el update: existe con ese atributo PERO con un ID distinto al que estoy editando
    boolean existsByVariantSkuIgnoreCaseAndVariantIdNot(String sku, Long variantId);

    // Buscar todas las variantes de un producto
    @EntityGraph(attributePaths = {"product", "color"})
    List<Variant> findAllByProduct_ProductId(UUID productId);

    // Buscar variantes activas de un producto
    @EntityGraph(attributePaths = {"product", "color"})
    List<Variant> findAllByProduct_ProductIdAndRowStatus(UUID productId, RowStatus rowStatus);

    @Query("""
        SELECT v
        FROM Variant v
        JOIN FETCH v.product
        JOIN FETCH v.color
        WHERE v.variantId IN :variantIds
    """)
    List<Variant> findAllByIdInWithDetails(@Param("variantIds") Collection<Long> variantIds);

    @Query(value = """
        SELECT
            product_id AS "productId",
            product_name AS "productName",
            product_long_desc AS "productLongDesc",
            variant_id AS "variantId",
            variant_size::text AS "variantSize",
            variant_price AS "price",
            variant_stock AS "stock",
            color_id AS "colorId",
            color_name AS "colorName",
            color_code AS "colorCode",
            images::text AS "imagesJson"
        FROM v_public_product_variants
        WHERE product_id = :productId
        ORDER BY variant_size, color_id
        """, nativeQuery = true)
    List<PublicProductVariantProjection> findPublicVariantsByProductId(
            @Param("productId") UUID productId
    );

    interface PublicProductVariantProjection {
        UUID getProductId();
        String getProductName();
        String getProductLongDesc();
        Long getVariantId();
        String getVariantSize();
        BigDecimal getPrice();
        Integer getStock();
        Long getColorId();
        String getColorName();
        String getColorCode();
        String getImagesJson();
    }

    @Query("""
        SELECT COUNT(v)
        FROM Variant v
        WHERE v.product.productId = :productId
          AND v.product.rowStatus = :active
          AND v.color.colorId = :colorId
          AND v.color.rowStatus = :active
          AND v.rowStatus = :active
    """)
    long countActiveByProductIdAndColorId(
            @Param("productId") UUID productId,
            @Param("colorId") Long colorId,
            @Param("active") RowStatus active
    );

    /**
     * Marca como INACTIVE solo las variantes ACTIVE del producto indicado
     * @param productId productId
     */
    default void inactivateVariantsByProductId(@Param("productId") UUID productId) {
        inactivateVariantsByProductId(productId, RowStatus.ACTIVE, RowStatus.INACTIVE);
    }
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE
            Variant v
        SET
            v.rowStatus = :inactive
        WHERE
            v.product.productId = :productId
            AND v.rowStatus     = :active
    """)
    void inactivateVariantsByProductId(
            @Param("productId") UUID        productId,
            @Param("active")    RowStatus   active,
            @Param("inactive")  RowStatus   inactive
    );

    /**
     * Marca como DELETED todas las variantes del producto indicado
     * @param productId productId
     */
    default void deleteVariantsByProductId(@Param("productId") UUID productId) {
        deleteVariantsByProductId(productId, RowStatus.DELETED);
    }    
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE  Variant v
        SET     v.rowStatus         = :deleted
        WHERE   v.product.productId = :productId
    """)
    void deleteVariantsByProductId(
            @Param("productId") UUID        productId,
            @Param("deleted")   RowStatus   deleted
    );
    
    // =========================================================================
    // Concurrencia y Control de Stock (Pessimistic Locking: SELECT FOR UPDATE)
    // =========================================================================

    /**
     * Bloquea la fila de la variante a nivel base de datos hasta que termine la transacción.
     * Evita que dos compras concurrentes descuenten stock sobre el mismo registro a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT  v 
        FROM    Variant v 
        WHERE   v.variantId = :variantId
    """)
    Optional<Variant> findByIdWithLock(@Param("variantId") Long variantId);

    /**
     * Bloquea en lote todas las variantes involucradas en un checkout.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT  v 
        FROM    Variant v
        JOIN FETCH v.product
        JOIN FETCH v.color
        WHERE   v.variantId IN :variantIds
    """)
    List<Variant> findAllByIdInWithLock(@Param("variantIds") Collection<Long> variantIds);
}