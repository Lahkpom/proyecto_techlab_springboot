package com.proyecto.proyecto_techlab_springboot.sale.scheduler;

import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.sale.repository.ISaleRepository;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Busca órdenes vencidas en PENDIENTE y órdenes en preparación pagadas en efectivo.
 * Invoca variantService.restoreStock(...) de forma transaccional.
 * Transiciona a CANCELADO y registra una nota en internalNotes.
 */
@Slf4j
@Component
public class SaleExpirationScheduler {

    private final ISaleRepository   iSaleRepository;
    private final VariantService    variantService;
    private final long ttlHours;
    private final long cashTtlHours;

    // Horas de tolerancia antes de cancelar por abandono (48hs)
    public SaleExpirationScheduler(
            ISaleRepository iSaleRepository,
            VariantService variantService,
            @Value("${sales.expiration.ttl-hours:48}") long ttlHours,
            @Value("${sales.expiration.cash-ttl-hours:72}") long cashTtlHours
    ) {
        this.iSaleRepository = iSaleRepository;
        this.variantService = variantService;
        this.ttlHours = ttlHours;
        this.cashTtlHours = cashTtlHours;
    }

    /**
     * Tarea programada diaria: corre a las 03:00 AM hora local.
     * Busca pedidos PENDIENTE vencidos a las ttlHours y pedidos en preparación en efectivo
     * vencidos a las cashTtlHours, y restituye su stock.
     */
    @Scheduled(
            cron = "${sales.expiration.cron:0 0 3 * * *}",
            zone = "${sales.expiration.zone:America/Argentina/Buenos_Aires}"
    )
    @Transactional
    public void cancelExpiredPendingSales() {
        List<Sale> expiredPendingSales = this.iSaleRepository.findExpiredPendingSales(
                SaleStatus.PENDIENTE,
                Instant.now().minus(Duration.ofHours(this.ttlHours))
        );
        List<Sale> expiredCashPreparationSales = this.iSaleRepository.findExpiredCashPreparationSales(
                SaleStatus.PREPARACION,
                PaymentMethod.EFECTIVO,
                PaymentStatus.PENDIENTE,
                Instant.now().minus(Duration.ofHours(this.cashTtlHours))
        );

        if (expiredPendingSales.isEmpty() && expiredCashPreparationSales.isEmpty()) {
            log.info("Purga nocturna TTL: No se registraron órdenes vencidas.");
            return;
        }

        CancellationBatchResult pendingResult = cancelExpiredSales(
                expiredPendingSales,
                "[SISTEMA - PURGA NOCTURNA]: Cancelada por superar el límite de %d horas sin confirmación de pago."
                        .formatted(ttlHours)
        );
        CancellationBatchResult cashResult = cancelExpiredSales(
                expiredCashPreparationSales,
                "[SISTEMA - PURGA NOCTURNA]: Cancelada por superar el límite de %d horas en preparación sin coordinar/entregar el pedido en efectivo."
                        .formatted(cashTtlHours)
        );
        log.info(
                "Purga nocturna TTL finalizada: encontradas={}, canceladas={}, fallidas={}",
                expiredPendingSales.size() + expiredCashPreparationSales.size(),
                pendingResult.canceled() + cashResult.canceled(),
                pendingResult.failed() + cashResult.failed()
        );
    }

    private CancellationBatchResult cancelExpiredSales(List<Sale> expiredSales, String auditMessage) {
        int canceled = 0;
        int failed = 0;
        for (Sale sale : expiredSales) {
            try {
                Map<Long, Integer> stockMap = sale.getItems().stream()
                        .filter(item -> item.getVariant() != null)
                        .collect(Collectors.toMap(
                                item -> item.getVariant().getVariantId(),
                                SaleItem::getQuantity,
                                Integer::sum
                        ));

                this.variantService.restoreStock(stockMap);

                sale.setSaleStatus(SaleStatus.CANCELADO);
                sale.setPaymentStatus(PaymentStatus.RECHAZADO);
                sale.setUpdatedAt(Instant.now());

                String currentNotes = sale.getInternalNotes() != null ? sale.getInternalNotes() + "\n" : "";
                sale.setInternalNotes(currentNotes + auditMessage);
                canceled++;
            } catch (Exception ex) {
                log.warn("No se pudo cancelar la venta vencida: saleId={}", sale.getSaleId(), ex);
                failed++;
            }
        }
        return new CancellationBatchResult(canceled, failed);
    }

    private record CancellationBatchResult(int canceled, int failed) {}
}