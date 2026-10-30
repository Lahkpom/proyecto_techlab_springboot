package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.SaleReturn;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ISaleReturnRepository extends JpaRepository<SaleReturn, UUID> {

    /**
     * Consulta pública segura mediante Token de Seguimiento (Magic Link) para clientes.
     */
    Optional<SaleReturn> findByTrackingToken(UUID trackingToken);

    /**
     * Historial de solicitudes de posventa asociadas a una orden de compra.
     */
    List<SaleReturn> findAllBySale_SaleIdOrderByCreatedAtDesc(UUID saleId);

    /**
     * Historial de devoluciones de un usuario registrado.
     */
    Page<SaleReturn> findAllBySale_User_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Valida si existe un trámite de devolución activo sobre la venta para evitar solicitudes concurrentes duplicadas.
     */
    boolean existsBySale_SaleIdAndReturnStatusIn(UUID saleId, Collection<ReturnStatus> activeStatuses);

    /**
     * Recupera el detalle completo con Sale e Items precargados para evitar N+1 queries en inspección.
     */
    @Query("""
        SELECT sr FROM SaleReturn sr
        JOIN FETCH sr.sale s
        LEFT JOIN FETCH sr.items sri
        LEFT JOIN FETCH sri.saleItem si
        WHERE sr.returnId = :returnId
    """)
    Optional<SaleReturn> findByIdWithDetails(@Param("returnId") UUID returnId);

    /**
     * Búsqueda dinámica multicriterio para el panel administrativo.
     */
    @Query("""
        SELECT sr FROM SaleReturn sr
        JOIN sr.sale s
        WHERE (:returnStatus IS NULL OR sr.returnStatus = :returnStatus)
          AND (:returnReason IS NULL OR sr.returnReason = :returnReason)
          AND (:customerEmail IS NULL OR LOWER(s.customerEmail) LIKE LOWER(CONCAT('%', :customerEmail, '%')))
          AND (:startDate IS NULL OR sr.createdAt >= :startDate)
          AND (:endDate IS NULL OR sr.createdAt <= :endDate)
        ORDER BY sr.createdAt DESC
    """)
    Page<SaleReturn> searchReturnsAdmin(
            @Param("returnStatus")  ReturnStatus    returnStatus,
            @Param("returnReason")  ReturnReason    returnReason,
            @Param("customerEmail") String          customerEmail,
            @Param("startDate")     Instant         startDate,
            @Param("endDate")       Instant         endDate,
            Pageable pageable
    );
}