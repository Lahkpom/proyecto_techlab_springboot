package com.proyecto.proyecto_techlab_springboot.scheduler;

import com.proyecto.proyecto_techlab_springboot.model.Sale;
import com.proyecto.proyecto_techlab_springboot.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleRepository;
import com.proyecto.proyecto_techlab_springboot.service.VariantService;
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
 * Ejecuta a intervalos regulares (por ejemplo, cada 15 o 30 minutos).
 * Busca órdenes con más de $X$ horas en PENDIENTE.
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

    /**
     * Tarea programada diaria: corre a las 03:00 AM hora local.
     * Busca pedidos en PENDIENTE creados hace más de ttlHours y restituye stock.
     */
    @Scheduled(
            cron = "${sales.expiration.cron:0 0 3 * * *}",
            zone = "${sales.expiration.zone:America/Argentina/Buenos_Aires}"
    )
    @Transactional
    public void cancelExpiredPendingSales() {
        List<Sale> expiredSales = this.iSaleRepository.findExpiredPendingSales(
                Instant.now().minus(Duration.ofHours(this.ttlHours))
        );

        if (expiredSales.isEmpty()) {
            log.info("Purga nocturna TTL: No se registraron órdenes pendientes vencidas.");
            return;
        }

        log.info("Iniciando purga nocturna TTL. Cancelando {} orden(es) abandonada(s)...", expiredSales.size());

        for (Sale sale : expiredSales) {
            try {
                // Restituir el stock de las prendas
                Map<Long, Integer> stockMap = sale.getItems().stream()
                        .filter(item -> item.getVariant() != null)
                        .collect(Collectors.toMap(
                                item -> item.getVariant().getVariantId(),
                                SaleItem::getQuantity,
                                Integer::sum
                        ));

                this.variantService.restoreStock(stockMap);

                // Cancelar la orden
                sale.setSaleStatus(SaleStatus.CANCELADO);
                sale.setPaymentStatus(PaymentStatus.RECHAZADO);
                sale.setUpdatedAt(Instant.now());

                String auditMessage = "[SISTEMA - PURGA NOCTURNA]: Cancelada por superar el límite de %d horas sin confirmación de pago."
                                .formatted(ttlHours);
                String currentNotes = sale.getInternalNotes() != null ? sale.getInternalNotes() + "\n" : "";
                sale.setInternalNotes(currentNotes + auditMessage);

                log.info("Orden ID: {} cancelada y stock devuelto al inventario con éxito.", sale.getSaleId());
            } catch (Exception ex) {
                log.error("Fallo al purgar la orden ID: {}: {}", sale.getSaleId(), ex.getMessage());
            }
        }
    }
}