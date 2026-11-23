package com.proyecto.proyecto_techlab_springboot.salereturn.repository;

import com.proyecto.proyecto_techlab_springboot.salereturn.model.SaleReturn;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * Historial de solicitudes de posventa asociadas a una orden de compra con sus ítems precargados.
     */
    @Query("""
        SELECT DISTINCT sr FROM SaleReturn sr
        JOIN FETCH sr.sale s
        LEFT JOIN FETCH sr.items sri
        LEFT JOIN FETCH sri.saleItem si
        WHERE s.saleId = :saleId
        ORDER BY sr.createdAt DESC
    """)
    List<SaleReturn> findAllBySale_SaleIdOrderByCreatedAtDesc(@Param("saleId") UUID saleId);

    /**
     * Historial de devoluciones de un usuario registrado.
     */
    @EntityGraph(attributePaths = "sale")
    Page<SaleReturn> findAllBySale_User_UserIdOrderByCreatedAtDescReturnIdAsc(UUID userId, Pageable pageable);

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
    @EntityGraph(attributePaths = "sale")
    @Query(value = """
        SELECT sr FROM SaleReturn sr
        JOIN sr.sale s
        WHERE (:filterReturnStatus = false OR sr.returnStatus = :returnStatus)
          AND (:filterReturnReason = false OR sr.returnReason = :returnReason)
          AND LOWER(s.customerEmail) LIKE LOWER(:customerEmailPattern)
          AND (:filterStartDate = false OR sr.createdAt >= :startDate)
          AND (:filterEndDate = false OR sr.createdAt <= :endDate)
        ORDER BY sr.createdAt DESC, sr.returnId ASC
    """, countQuery = """
        SELECT COUNT(sr) FROM SaleReturn sr
        JOIN sr.sale s
        WHERE (:filterReturnStatus = false OR sr.returnStatus = :returnStatus)
          AND (:filterReturnReason = false OR sr.returnReason = :returnReason)
          AND LOWER(s.customerEmail) LIKE LOWER(:customerEmailPattern)
          AND (:filterStartDate = false OR sr.createdAt >= :startDate)
          AND (:filterEndDate = false OR sr.createdAt <= :endDate)
    """)
    Page<SaleReturn> searchReturnsAdmin(
            @Param("filterReturnStatus") boolean filterReturnStatus,
            @Param("returnStatus")  ReturnStatus    returnStatus,
            @Param("filterReturnReason") boolean filterReturnReason,
            @Param("returnReason")  ReturnReason    returnReason,
            @Param("customerEmailPattern") String   customerEmailPattern,
            @Param("filterStartDate") boolean filterStartDate,
            @Param("startDate")     Instant         startDate,
            @Param("filterEndDate") boolean filterEndDate,
            @Param("endDate")       Instant         endDate,
            Pageable pageable
    );
}