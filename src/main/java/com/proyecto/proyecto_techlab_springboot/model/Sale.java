package com.proyecto.proyecto_techlab_springboot.model;

import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "sale")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sale_id", updatable = false, nullable = false)
    private UUID saleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Builder.Default
    @Column(name = "tracking_token", nullable = false, unique = true, updatable = false)
    private UUID trackingToken = UUID.randomUUID();

    // Snapshot del comprador
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_phone", length = 50, nullable = false)
    private String customerPhone;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "shipping_address", columnDefinition = "TEXT", nullable = false)
    private String shippingAddress;

    // Totales monetarios
    @Column(name = "sale_quantity", nullable = false)
    private Integer saleQuantity;

    @Column(name = "sale_subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal saleSubtotal;

    @Builder.Default
    @Column(name = "sale_discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal saleDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "shipping_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "sale_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal saleTotal;

    // Fechas de despacho
    @Column(name = "sale_shipped_at")
    private Instant saleShippedAt;

    @Column(name = "sale_delivered_at")
    private Instant saleDeliveredAt;

    // Máquinas de estado
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false)
    private SaleStatus saleStatus = SaleStatus.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDIENTE;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    // Pasarela de pago (Mercado Pago)
    @Column(name = "mp_payment_id", length = 100)
    private String mpPaymentId;

    @Column(name = "mp_preference_id", length = 100)
    private String mpPreferenceId;

    @Column(name = "mp_merchant_order_id", length = 100)
    private String mpMerchantOrderId;

    // Auditoría
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Relación con ítems
    @Builder.Default
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }
}