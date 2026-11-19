package com.proyecto.proyecto_techlab_springboot.shipping.model;

import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "shipping_zone")
// TODO - Hacer que esta tabla extienda de BaseEntity, agregarle created_by y ponerlo junto con Updated_by en el BaseEntity
public class ShippingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_zone_id")
    private Long shippingZoneId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "shipping_method", nullable = false)
    private ShippingMethod shippingMethod;

    @Column(name = "zone_name", length = 120, nullable = false)
    private String zoneName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "postal_codes", columnDefinition = "jsonb", nullable = false)
    private List<String> postalCodes;

    @Column(name = "base_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal baseCost;

    @Column(name = "estimated_days", length = 80, nullable = false)
    private String estimatedDays;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "row_status", nullable = false)
    private RowStatus rowStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
