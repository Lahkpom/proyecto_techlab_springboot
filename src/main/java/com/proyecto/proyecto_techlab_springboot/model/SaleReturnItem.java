package com.proyecto.proyecto_techlab_springboot.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "sale_return_item")
public class SaleReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id", updatable = false, nullable = false)
    private Long returnItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private SaleReturn saleReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_item_id", nullable = false, updatable = false)
    private SaleItem saleItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "restock_approved")
    private Boolean restockApproved;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}