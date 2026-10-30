package com.proyecto.proyecto_techlab_springboot.model;

import com.proyecto.proyecto_techlab_springboot.model.enums.NumericSize;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "sale_item")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_item_id", updatable = false, nullable = false)
    private Long saleItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private Variant variant;

    // Snapshot inmutable de catálogo
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_desc", nullable = false)
    private String productDesc;

    @Column(name = "variant_desc", nullable = false)
    private String variantDesc;

    @Column(name = "variant_sku", length = 100, nullable = false)
    private String variantSku;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "variant_size", nullable = false)
    private NumericSize variantSize;

    @Column(name = "color_name", length = 100, nullable = false)
    private String colorName;

    @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    // Valores económicos
    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}