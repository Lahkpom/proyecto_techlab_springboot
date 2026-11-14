package com.proyecto.proyecto_techlab_springboot.sale.service;

import com.proyecto.proyecto_techlab_springboot.sale.client.IMercadoPagoClient;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.fiscal.service.FiscalReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoWebhookService {

    private final IMercadoPagoClient   iMercadoPagoClient;
    private final SaleService          saleService;
    private final FiscalReceiptService fiscalReceiptService;

    @Transactional
    public void processNotification(String xSignature, String action, String topic, String dataId) {
        // En Mercado Pago el ID del pago puede venir en data.id o por query param según la versión de evento
        if (dataId == null || dataId.isBlank()) {
            log.warn("Notificación de Webhook recibida sin identificador de recurso. Se ignora.");
            return;
        }

        // Validar firma criptográfica HMAC-SHA256
        this.iMercadoPagoClient.validateWebhookSignature(xSignature, dataId);

        // Solo procesamos eventos asociados a pagos
        if (!"payment".equalsIgnoreCase(topic) && !"payment.created".equalsIgnoreCase(action) && !"payment.updated".equalsIgnoreCase(action)) {
            log.info("Notificación recibida para tópico no procesable: {}. Se descarta con éxito.", topic);
            return;
        }

        Long    paymentId       = Long.valueOf(dataId);
        String  stringPaymentId = String.valueOf(paymentId);

        // Consultar a la API de Mercado Pago para verificar autenticidad y montos
        IMercadoPagoClient.MercadoPagoPaymentDetails payment = this.iMercadoPagoClient.getPaymentDetails(paymentId);
        String externalReference = payment.externalReference();

        if (externalReference == null || externalReference.isBlank()) {
            log.error("El pago {} de MP no contiene externalReference vinculada a una orden.", paymentId);
            return;
        }

        UUID saleId = UUID.fromString(externalReference);
        Sale sale   = this.saleService.getSaleByIdOrElseThrow(saleId);

        // Traducir el estado de MP a nuestro enum de dominio
        PaymentStatus targetPaymentStatus = mapMercadoPagoStatus(payment.status());
        if (targetPaymentStatus == null) {
            log.warn("El pago MP ID: {} posee un estado no gestionable ('{}'). Se descarta.", paymentId, payment.status());
            return;
        }

        // VALIDACIÓN DE MONTO (Amount Tampering) para pagos aprobados
        if (targetPaymentStatus == PaymentStatus.APROBADO) {
            BigDecimal paidAmount       = payment.transactionAmount();
            BigDecimal expectedAmount   = sale.getSaleTotal();

            if (paidAmount == null || expectedAmount.compareTo(paidAmount) != 0) {
                String warningMessage =
                        "[AUDITORÍA AUTOMÁTICA]: El monto cobrado por Mercado Pago ($%s) no coincide con el total de la orden ($%s). MP Payment ID: %s"
                                .formatted(paidAmount, expectedAmount, stringPaymentId);
                log.error("Discrepancia financiera detectada en Venta ID: {}. Reteniendo en AUDITORIA. {}", saleId, warningMessage);

                sale.setSaleStatus(SaleStatus.AUDITORIA);
                sale.setMpPaymentId(stringPaymentId);
                sale.setUpdatedAt(Instant.now());

                String currentNotes = sale.getInternalNotes() != null ? sale.getInternalNotes() + "\n" : "";
                sale.setInternalNotes(currentNotes + warningMessage);
                
                // Finalizamos la ejecución del webhook con éxito hacia MP, dejando la orden retenida en la BD
                return;
            }
        }

        // FILTRO DE IDEMPOTENCIA: Si ya está procesado, no hacemos nada
        if (sale.hasSamePaymentState(stringPaymentId, targetPaymentStatus)) {
            log.info("Notificación duplicada ignorada (Idempotencia). Venta ID: {} ya se encuentra en estado {} para MP Payment ID: {}",
                    saleId, targetPaymentStatus, stringPaymentId);
            return;
        }

        log.info("Aplicando actualización por Webhook: Venta ID: {}, MP Payment ID: {}, Nuevo PaymentStatus: {}",
                saleId, stringPaymentId, targetPaymentStatus);

        sale.setMpPaymentId(stringPaymentId);
        sale.setPaymentStatus(targetPaymentStatus);
        sale.setUpdatedAt(Instant.now());

        // Transición cruzada a nivel logístico
        if (targetPaymentStatus == PaymentStatus.APROBADO && sale.getSaleStatus() == SaleStatus.PENDIENTE) {
            sale.setSaleStatus(SaleStatus.PREPARACION);
            // DISPARO FISCAL: Emisión de Factura C ante pago aprobado en Mercado Pago
            this.fiscalReceiptService.tryEmitInvoiceSafely(saleId, null);
        } else if (targetPaymentStatus == PaymentStatus.REEMBOLSADO) {
            sale.setSaleStatus(SaleStatus.REEMBOLSADO);
        }
    }

    private PaymentStatus mapMercadoPagoStatus(String mpStatus) {
        if (mpStatus == null) return null;

        return switch (mpStatus.toLowerCase()) {
            case "approved" -> PaymentStatus.APROBADO;
            case "rejected", "cancelled" -> PaymentStatus.RECHAZADO;
            case "refunded", "charged_back" -> PaymentStatus.REEMBOLSADO;
            case "in_process", "pending", "authorized" -> PaymentStatus.PENDIENTE;
            default -> null;
        };
    }
}