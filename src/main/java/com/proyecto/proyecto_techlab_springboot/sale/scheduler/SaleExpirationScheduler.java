package com.proyecto.proyecto_techlab_springboot.sale.scheduler;

import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.sale.repository.ISaleRepository;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SaleExpirationScheduler {

    private final ISaleRepository   iSaleRepository;
    private final VariantService    variantService;

    // Horas de tolerancia antes de cancelar por abandono (48hs)
    @Value("${sales.expiration.ttl-hours:48}")
    private long ttlHours;

    @Value("${sales.expiration.cash-ttl-hours:72}")
    private long cashTtlHours;

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
                Instant.now().minus(Duration.ofHours(this.ttlHours))
        );
        List<Sale> expiredCashPreparationSales = this.iSaleRepository.findExpiredCashPreparationSales(
                Instant.now().minus(Duration.ofHours(this.cashTtlHours))
        );

        if (expiredPendingSales.isEmpty() && expiredCashPreparationSales.isEmpty()) {
            log.info("Purga nocturna TTL: No se registraron órdenes vencidas.");
            return;
        }

        cancelExpiredSales(
                expiredPendingSales,
                "[SISTEMA - PURGA NOCTURNA]: Cancelada por superar el límite de %d horas sin confirmación de pago."
                        .formatted(ttlHours)
        );
        cancelExpiredSales(
                expiredCashPreparationSales,
                "[SISTEMA - PURGA NOCTURNA]: Cancelada por superar el límite de %d horas en preparación sin coordinar/entregar el pedido en efectivo."
                        .formatted(cashTtlHours)
        );
    }

    private void cancelExpiredSales(List<Sale> expiredSales, String auditMessage) {
        for (Sale sale : expiredSales) {
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

            log.info("Orden ID: {} cancelada y stock devuelto al inventario con éxito.", sale.getSaleId());
        }
    }
}