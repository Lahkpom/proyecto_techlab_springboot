package com.proyecto.proyecto_techlab_springboot.model;

import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@MappedSuperclass
@SQLRestriction("row_status <> 'DELETED'") // <- MAGIA NEGRA DE HIBERNATE - Esto es para que nunca traiga registros con RowStatus.DELETED
public abstract class BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "row_status", nullable = false)
    protected RowStatus rowStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt;

    @Column(name = "updated_at")
    protected Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.rowStatus = RowStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public static void validateRowStatus(RowStatus rowStatus) {
        if (rowStatus == null)
            throw new NullPointerException("rowStatus cannot be null");
    }
    public static void validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null)
            throw new NullPointerException("createdAt cannot be null");
    }
}