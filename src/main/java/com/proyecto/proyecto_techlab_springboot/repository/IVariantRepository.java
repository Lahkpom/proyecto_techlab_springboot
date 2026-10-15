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
    List<Variant> findAllByProduct_ProductId(UUID productId);

    // Buscar variantes activas de un producto
    List<Variant> findAllByProduct_ProductIdAndRowStatus(UUID productId, RowStatus rowStatus);

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
}