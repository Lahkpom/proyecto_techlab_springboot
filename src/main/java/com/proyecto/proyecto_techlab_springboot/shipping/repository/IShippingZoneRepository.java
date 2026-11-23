package com.proyecto.proyecto_techlab_springboot.shipping.repository;

import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingMethod;
import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IShippingZoneRepository extends JpaRepository<ShippingZone, Long> {

    Optional<ShippingZone> findFirstByShippingMethodAndRowStatus(
            ShippingMethod method,
            RowStatus status
    );

    List<ShippingZone> findAllByRowStatusOrderByBaseCostAsc(RowStatus status);

    List<ShippingZone> findAllByRowStatusNotOrderByBaseCostAsc(RowStatus status);

    @Query(value = """
        SELECT * FROM shipping_zone
        WHERE row_status = 'ACTIVE'
          AND postal_codes @> CAST(:postalCodeJson AS jsonb)
        ORDER BY shipping_zone_id ASC
        LIMIT 1
    """, nativeQuery = true)
    Optional<ShippingZone> findActiveZoneByPostalCode(@Param("postalCodeJson") String postalCodeJson);
}
