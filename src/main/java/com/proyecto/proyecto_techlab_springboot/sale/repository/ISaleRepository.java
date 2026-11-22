package com.proyecto.proyecto_techlab_springboot.sale.repository;

import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceiptType;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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

    @Query("""
        SELECT s
        FROM Sale s
        WHERE s.user.userId = :userId
          AND (:filterStatuses = false OR s.saleStatus IN :statuses)
        ORDER BY s.createdAt DESC
    """)
    Page<Sale> findMyOrdersByStatuses(
            @Param("userId") UUID userId,
            @Param("filterStatuses") boolean filterStatuses,
            @Param("statuses") List<SaleStatus> statuses,
            Pageable pageable
    );

    // Trae por estado de pago, se crea para ser usado por el scheduler de reintento de facturación (Paginado)
    Page<Sale> findAllByPaymentStatusOrderByCreatedAtAsc(PaymentStatus paymentStatus, Pageable pageable);

    /**
     * Trae las ventas con estado de pago aprobado que no tienen aún su registro en fiscal_receipt.
     * Es utilizada por el scheduler encargado de reintentar las emisiones de facturas
     */
    @Query("""
        SELECT s FROM Sale s
        WHERE s.paymentStatus = :paymentStatus
          AND s.fiscalRejection = false
          AND NOT EXISTS (
              SELECT 1 FROM FiscalReceipt fr
              WHERE fr.sale = s
                AND fr.receiptType = :receiptType
          )
        ORDER BY s.createdAt ASC
    """)
    List<Sale> findUnbilledSalesByPaymentStatus(
            @Param("paymentStatus") PaymentStatus       paymentStatus,
            @Param("receiptType")   FiscalReceiptType   receiptType,
            Pageable pageable
    );

    // Reconciliación con Mercado Pago (Webhooks y Redirecciones)
    Optional<Sale> findByMpPreferenceId(String mpPreferenceId);
    Optional<Sale> findByMpPaymentId(String mpPaymentId);

    // Para Panel Administrativo: Búsqueda dinámica multicriterio
    @Query("""
        SELECT s
        FROM Sale s
        WHERE (:auditOnly = false OR (
                s.saleStatus = :auditStatus
                OR (s.fiscalRejection = true AND s.saleStatus NOT IN (
                    :terminalStatuses
                ))
              ))
          AND (:filterStatuses = false OR s.saleStatus IN :statuses)
          AND (:filterSaleStatus = false OR s.saleStatus = :saleStatus)
          AND (:filterPaymentStatus = false OR s.paymentStatus = :paymentStatus)
          AND (:filterFiscalRejection = false OR s.fiscalRejection = :fiscalRejection)
          AND LOWER(s.customerEmail) LIKE LOWER(:customerEmailPattern)
          AND (:filterStartDate = false OR s.createdAt >= :startDate)
          AND (:filterEndDate = false OR s.createdAt <= :endDate)
        ORDER BY s.createdAt DESC
    """)
    Page<Sale> searchSalesAdmin(
            @Param("auditOnly") boolean auditOnly,
            @Param("auditStatus") SaleStatus auditStatus,
            @Param("terminalStatuses") List<SaleStatus> terminalStatuses,
            @Param("filterStatuses") boolean filterStatuses,
            @Param("statuses") List<SaleStatus> statuses,
            @Param("filterSaleStatus") boolean filterSaleStatus,
            @Param("saleStatus")    SaleStatus      saleStatus,
            @Param("filterPaymentStatus") boolean filterPaymentStatus,
            @Param("paymentStatus") PaymentStatus   paymentStatus,
            @Param("filterFiscalRejection") boolean filterFiscalRejection,
            @Param("fiscalRejection") Boolean fiscalRejection,
            @Param("customerEmailPattern") String   customerEmailPattern,
            @Param("filterStartDate") boolean filterStartDate,
            @Param("startDate")     Instant         startDate,
            @Param("filterEndDate") boolean filterEndDate,
            @Param("endDate")       Instant         endDate,
            Pageable pageable
    );

    /**
     * Recupera órdenes que permanecen en PENDIENTE tras haber superado el umbral de expiración,
     * excluyendo métodos de pago manuales que requieran revisión humana prolongada si correspondiera.
     */
    @Query("""
        SELECT s FROM Sale s
        WHERE s.saleStatus = :saleStatus
          AND s.createdAt < :threshold
    """)
    List<Sale> findExpiredPendingSales(
            @Param("saleStatus") SaleStatus saleStatus,
            @Param("threshold") Instant threshold
    );

    @Query("""
        SELECT s FROM Sale s
        WHERE s.saleStatus = :saleStatus
          AND s.paymentMethod = :paymentMethod
          AND s.paymentStatus = :paymentStatus
          AND s.createdAt < :threshold
    """)
    List<Sale> findExpiredCashPreparationSales(
            @Param("saleStatus") SaleStatus saleStatus,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("threshold") Instant threshold
    );

    // Para simulación en perfil DEV: obtiene la última orden pendiente de Mercado Pago
    Optional<Sale> findFirstByPaymentMethodAndPaymentStatusOrderByCreatedAtDesc(
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus
    );
}