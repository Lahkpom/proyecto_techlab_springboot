package com.proyecto.proyecto_techlab_springboot.model;

import com.proyecto.proyecto_techlab_springboot.model.enums.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product")
public class Product extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "product_category", nullable = false)
    private Category productCategory;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_short_desc", nullable = false)
    private String productShortDesc;

    @Column(name = "product_long_desc", nullable = false, columnDefinition = "TEXT")
    private String productLongDesc;
}