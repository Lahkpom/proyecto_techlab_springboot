package com.proyecto.proyecto_techlab_springboot.product.repository;

import com.proyecto.proyecto_techlab_springboot.product.model.Product;
import com.proyecto.proyecto_techlab_springboot.product.model.TargetGender;
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

    @Query(
        value = """
            SELECT p
            FROM Product p
            WHERE (:filterGender = false OR p.targetGender = :targetGender)
              AND (:filterStatus = false OR p.rowStatus = :rowStatus)
              AND (LOWER(p.productName) LIKE LOWER(:searchPattern)
                   OR LOWER(p.productShortDesc) LIKE LOWER(:searchPattern)
                   OR LOWER(p.productLongDesc) LIKE LOWER(:searchPattern))
            ORDER BY CASE WHEN p.rowStatus = :activeStatus THEN 0 ELSE 1 END ASC,
                     p.targetGender ASC,
                     p.productName ASC
        """,
        countQuery = """
            SELECT COUNT(p)
            FROM Product p
            WHERE (:filterGender = false OR p.targetGender = :targetGender)
              AND (:filterStatus = false OR p.rowStatus = :rowStatus)
              AND (LOWER(p.productName) LIKE LOWER(:searchPattern)
                   OR LOWER(p.productShortDesc) LIKE LOWER(:searchPattern)
                   OR LOWER(p.productLongDesc) LIKE LOWER(:searchPattern))
        """
    )
    Page<Product> searchAdminProducts(
            @Param("filterGender") boolean filterGender,
            @Param("targetGender") TargetGender targetGender,
            @Param("filterStatus") boolean filterStatus,
            @Param("rowStatus") RowStatus rowStatus,
            @Param("searchPattern") String searchPattern,
            @Param("activeStatus") RowStatus activeStatus,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Product p
        ORDER BY CASE WHEN p.rowStatus = :activeStatus THEN 0 ELSE 1 END ASC,
                 p.productName ASC
    """)
    List<Product> findAllOptionsForSelect(@Param("activeStatus") RowStatus activeStatus);

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