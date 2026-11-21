package com.proyecto.proyecto_techlab_springboot.product.repository;

import com.proyecto.proyecto_techlab_springboot.product.model.Product;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface IProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByProductNameIgnoreCase(String productName);
    boolean existsByProductNameIgnoreCaseAndProductIdNot(String productName, UUID productId);

    // Para Administradores: Trae ACTIVE e INACTIVE
    @Query("""
        SELECT p FROM Product p
        ORDER BY p.createdAt DESC
    """)
    Page<Product> findAllForAdmin(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.rowStatus = com.proyecto.proyecto_techlab_springboot.common.model.RowStatus.ACTIVE ORDER BY p.productName ASC")
    List<Product> findAllActiveForSelect();

    @Query(
        value = """
            SELECT
                product_id       AS "productId",
                product_category AS "productCategory",
                target_gender    AS "targetGender",
                product_name     AS "productName",
                short_desc       AS "shortDesc",
                min_price        AS "minPrice",
                size_count       AS "sizeCount",
                color_count      AS "colorCount",
                main_images_url::text AS "mainImagesUrl"
            FROM v_public_product_catalog
            WHERE CAST(target_gender AS text) = :targetGender
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM v_public_product_catalog
            WHERE CAST(target_gender AS text) = :targetGender
            """,
        nativeQuery = true
    )
    Page<PublicProductCatalogProjection> findPublicCatalog(
            @Param("targetGender") String targetGender,
            Pageable pageable
    );

    interface PublicProductCatalogProjection {
        UUID getProductId();
        String getProductCategory();
        String getTargetGender();
        String getProductName();
        String getShortDesc();
        BigDecimal getMinPrice();
        Long getSizeCount();
        Long getColorCount();
        String getMainImagesUrl();
    }
}