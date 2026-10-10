package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface iVariantRepository extends JpaRepository<Variant, UUID> {

    @Modifying(clearAutomatically = true)
    @Query( "UPDATE Variant v " +
            "SET v.rowStatus = :newStatus " +
            "WHERE v.product.id = :productId " +
            "AND v.rowStatus = :currentStatus"
    )
    void updateStatusByProductId(
            @Param("productId")     UUID        productId,
            @Param("newStatus")     RowStatus   newStatus,
            @Param("currentStatus") RowStatus   currentStatus
    );
}