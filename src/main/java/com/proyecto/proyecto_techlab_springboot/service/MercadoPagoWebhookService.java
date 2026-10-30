package com.proyecto.proyecto_techlab_springboot.service;

import com.mercadopago.resources.payment.Payment;
import com.proyecto.proyecto_techlab_springboot.exception.sale.SaleNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Sale;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoWebhookService {

    private final MercadoPagoService    mercadoPagoService;
    private final ISaleRepository       iSaleRepository;

    @Transactional
    public void processNotification(String xSignature, String action, String topic, String dataId) {
        // En Mercado Pago el ID del pago puede venir en data.id o por query param según la versión de evento
        if (dataId == null || dataId.isBlank()) {
            log.warn("Notificación de Webhook recibida sin identificador de recurso. Se ignora.");
            return;
        }

        // Validar firma criptográfica HMAC-SHA256
        this.mercadoPagoService.validateWebhookSignature(xSignature, dataId);

        // Solo procesamos eventos asociados a pagos
        if (!"payment".equalsIgnoreCase(topic) && !"payment.created".equalsIgnoreCase(action) && !"payment.updated".equalsIgnoreCase(action)) {
            log.info("Notificación recibida para tópico no procesable: {}. Se descarta con éxito.", topic);
            return;
        }

        Long    paymentId       = Long.valueOf(dataId);
        String  stringPaymentId = String.valueOf(paymentId);

        // Consultar a la API de Mercado Pago para verificar autenticidad y montos
        Payment payment             = this.mercadoPagoService.getPaymentDetails(paymentId);
        String  externalReference   = payment.getExternalReference();

        if (externalReference == null || externalReference.isBlank()) {
            log.error("El pago {} de MP no contiene externalReference vinculada a una orden.", paymentId);
            return;
        }

        UUID saleId = UUID.fromString(externalReference);
        Sale sale   = this.iSaleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada para externalReference: " + saleId));

        // Traducir el estado de MP a nuestro enum de dominio
        PaymentStatus targetPaymentStatus = mapMercadoPagoStatus(payment.getStatus());
        if (targetPaymentStatus == null) {
            log.warn("El pago MP ID: {} posee un estado no gestionable ('{}'). Se descarta.", paymentId, payment.getStatus());
            return;
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
        if (targetPaymentStatus == PaymentStatus.APROBADO
                && sale.getSaleStatus() == SaleStatus.PENDIENTE
        ) {
            sale.setSaleStatus(SaleStatus.PREPARACION);
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