package com.proyecto.proyecto_techlab_springboot.fiscal.scheduler;

import com.proyecto.proyecto_techlab_springboot.fiscal.exception.ArcaUnavailableException;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceiptType;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.service.SaleService;
import com.proyecto.proyecto_techlab_springboot.fiscal.service.FiscalReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Corre automáticamente cada 15 minutos. Busca ventas con PaymentStatus.APROBADO que aún no tengan
 * una Factura C asociada en fiscal_receipt y reintenta la emisión de forma controlada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FiscalInvoiceRetryScheduler {

    private final SaleService           saleService;
    private final FiscalReceiptService  fiscalReceiptService;

    /**
     * Reintento de emisión cada 15 minutos en horario operativo.
     */
    // TODO - Poner estas variable en el .env y en el .properties
    @Scheduled(
            cron = "${arca.retry.cron:0 */15 * * * *}",
            zone = "${arca.retry.zone:America/Argentina/Buenos_Aires}"
    )
    public void retryPendingFiscalInvoices() {
        // Obtenemos ventas con pago aprobado pendientes de facturación en lotes de 20
        List<Sale> unbilledApprovedSales = this.saleService.findUnbilledSales(
                PaymentStatus.APROBADO,
                FiscalReceiptType.FACTURA_C,
                PageRequest.of(0, 20)
        );

        if (unbilledApprovedSales.isEmpty()) return;

        log.info("Scheduler Fiscal: Se encontraron {} venta(s) aprobada(s) sin Factura C. Reintentando...", unbilledApprovedSales.size());

        for (Sale sale : unbilledApprovedSales) {
            try {
                this.fiscalReceiptService.retryInvoiceFromScheduler(sale.getSaleId());
                // TODO - Me parece que este log ya lo hace el método de emisión, habría que sacarlo si es así
                log.info("Scheduler Fiscal: Factura C emitida con éxito para Venta ID: {}", sale.getSaleId());
            } catch (ArcaUnavailableException ex) {
                log.error("Scheduler Fiscal: ARCA se encuentra fuera de servicio. Se suspende el procesamiento del lote hasta la próxima ventana.");
                break; // Corta el bucle for de inmediato para no saturar al servidor caído
            } catch (Exception ex) {
                log.warn("Scheduler Fiscal: No se pudo emitir Factura C para Venta ID {}. Motivo: {}", sale.getSaleId(), ex.getMessage());
            }
        }
    }
}