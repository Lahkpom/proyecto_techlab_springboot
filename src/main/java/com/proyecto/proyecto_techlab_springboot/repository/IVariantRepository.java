package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.model.enums.NumericSize;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
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
        WHERE   v.variantId IN :variantIds
    """)
    List<Variant> findAllByIdInWithLock(@Param("variantIds") Collection<Long> variantIds);
}