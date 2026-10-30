package com.proyecto.proyecto_techlab_springboot.model;

import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapea la cabecera del reclamo manteniendo desacoplada la entidad Sale. Gestiona sus propios ítems mediante composición en cascada.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "sale_return")
public class SaleReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "return_id", updatable = false, nullable = false)
    private UUID returnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, updatable = false)
    private Sale sale;

    @Builder.Default
    @Column(name = "tracking_token", nullable = false, unique = true, updatable = false)
    private UUID trackingToken = UUID.randomUUID();

    // Clasificación y resolución
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "return_reason", nullable = false)
    private ReturnReason returnReason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "return_resolution", nullable = false)
    private ReturnResolution returnResolution = ReturnResolution.REEMBOLSO_DINERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "return_status", nullable = false)
    private ReturnStatus returnStatus = ReturnStatus.SOLICITADA;

    // Políticas de logística inversa
    @Builder.Default
    @Column(name = "shipping_covered_by_store", nullable = false)
    private Boolean shippingCoveredByStore = true;

    @Column(name = "return_shipping_guide", length = 100)
    private String returnShippingGuide;

    // Liquidación financiera
    @Builder.Default
    @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    // Declaración del cliente
    @Column(name = "customer_notes", columnDefinition = "TEXT", nullable = false)
    private String customerNotes;

    // Dictamen e inspección física en depósito
    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @Column(name = "inspection_evidence_path", columnDefinition = "TEXT")
    private String inspectionEvidencePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by")
    private Users inspectedBy;

    @Column(name = "inspected_at")
    private Instant inspectedAt;

    // Auditoría temporal
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    // Relación con renglones de devolución
    @Builder.Default
    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleReturnItem> items = new ArrayList<>();

    public void addItem(SaleReturnItem item) {
        items.add(item);
        item.setSaleReturn(this);
    }
}