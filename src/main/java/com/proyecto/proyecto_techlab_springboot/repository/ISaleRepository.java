package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Sale;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ISaleRepository extends JpaRepository<Sale, UUID> {

    /**
     * Acceso público para invitados (Magic Link / Seguimiento)
     * Los que inician compras sin tener una cuenta
     * 
     * @param trackingToken
     * @return
     */
    Optional<Sale> findByTrackingToken(UUID trackingToken);

    // Historial de compras de un usuario registrado (Paginado)
    Page<Sale> findAllByUser_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Reconciliación con Mercado Pago (Webhooks y Redirecciones)
    Optional<Sale> findByMpPreferenceId(String mpPreferenceId);
    Optional<Sale> findByMpPaymentId(String mpPaymentId);

    // Para Panel Administrativo: Búsqueda dinámica multicriterio
    @Query("""
        SELECT  s 
        FROM    Sale s
        WHERE 
            (:saleStatus        IS NULL OR s.saleStatus = :saleStatus)
            AND (:paymentStatus IS NULL OR s.paymentStatus = :paymentStatus)
            AND (:customerEmail IS NULL OR LOWER(s.customerEmail) LIKE LOWER(CONCAT('%', :customerEmail, '%')))
            AND (:startDate     IS NULL OR s.createdAt >= :startDate)
            AND (:endDate       IS NULL OR s.createdAt <= :endDate)
        ORDER BY 
            s.createdAt DESC
    """)
    Page<Sale> searchSalesAdmin(
            @Param("saleStatus")    SaleStatus      saleStatus,
            @Param("paymentStatus") PaymentStatus   paymentStatus,
            @Param("customerEmail") String          customerEmail,
            @Param("startDate")     Instant         startDate,
            @Param("endDate")       Instant         endDate,
            Pageable pageable
    );
}