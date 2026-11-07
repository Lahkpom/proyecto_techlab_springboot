package com.proyecto.proyecto_techlab_springboot.model;

import com.proyecto.proyecto_techlab_springboot.model.enums.FiscalReceiptType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "fiscal_receipt",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_receipt_number", columnNames = {"receipt_type", "point_of_sale", "receipt_number"})
        }
)
public class FiscalReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "fiscal_receipt_id", updatable = false, nullable = false)
    private UUID fiscalReceiptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false, updatable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", updatable = false)
    private SaleReturn saleReturn;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "receipt_type", nullable = false, updatable = false)
    private FiscalReceiptType receiptType;

    @Column(name = "afip_cbte_tipo", nullable = false, updatable = false)
    private Integer afipCbteTipo;

    @Column(name = "point_of_sale", nullable = false, updatable = false)
    private Integer pointOfSale;

    @Column(name = "receipt_number", nullable = false, updatable = false)
    private Long receiptNumber;

    @Column(name = "cae", length = 14, nullable = false, updatable = false)
    private String cae;

    @Column(name = "cae_due_date", nullable = false, updatable = false)
    private LocalDate caeDueDate;

    @Column(name = "issue_date", nullable = false, updatable = false)
    private LocalDate issueDate;

    // Snapshot del receptor
    @Column(name = "doc_type", nullable = false, updatable = false)
    private Integer docType;

    @Column(name = "doc_number", length = 20, nullable = false, updatable = false)
    private String docNumber;

    @Column(name = "customer_name", nullable = false, updatable = false)
    private String customerName;

    // Snapshot monetario
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false, updatable = false)
    private BigDecimal totalAmount;

    @Column(name = "net_amount", precision = 12, scale = 2, nullable = false, updatable = false)
    private BigDecimal netAmount;

    // Cadena/URL para código QR oficial de ARCA
    @Column(name = "qr_payload", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String qrPayload;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private Users createdBy;
}