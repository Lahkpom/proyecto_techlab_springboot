package com.proyecto.proyecto_techlab_springboot.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(
        name = "color",
        indexes = {
                @Index(name = "uk_color_name_not_deleted", columnList = "color_name"),
                @Index(name = "uk_color_code_not_deleted", columnList = "color_code")
        }
)
public class Color extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "color_id", nullable = false, updatable = false)
    private Long colorId;

    @Column(name = "color_name", nullable = false, length = 50)
    private String colorName;

    @Column(name = "color_desc", nullable = false, columnDefinition = "TEXT")
    private String colorDesc;

    @Column(name = "color_code", nullable = false, length = 7)
    private String colorCode;
}