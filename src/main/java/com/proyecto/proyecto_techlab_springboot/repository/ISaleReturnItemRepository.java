package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.SaleReturnItem;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ISaleReturnItemRepository extends JpaRepository<SaleReturnItem, Long> {

    List<SaleReturnItem> findAllBySaleReturn_ReturnId(java.util.UUID returnId);

    /**
     * Calcula la cantidad total de unidades ya reclamadas o devueltas para un renglón de venta (SaleItem),
     * excluyendo trámites cancelados o formalmente rechazados.
     * Esencial para evitar devoluciones acumuladas que superen la cantidad comprada.
     */
    @Query("""
        SELECT COALESCE(SUM(sri.quantity), 0)
        FROM SaleReturnItem sri
        JOIN sri.saleReturn sr
        WHERE sri.saleItem.saleItemId = :saleItemId
          AND sr.returnStatus NOT IN :excludedStatuses
    """)
    int countClaimedQuantityBySaleItemId(
            @Param("saleItemId") Long saleItemId,
            @Param("excludedStatuses") Collection<ReturnStatus> excludedStatuses
    );
}