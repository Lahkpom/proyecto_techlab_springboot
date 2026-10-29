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

        Long paymentId = Long.valueOf(dataId);

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

        sale.setMpPaymentId(String.valueOf(paymentId));
        sale.setUpdatedAt(Instant.now());

        // Mapeo de estados de Mercado Pago
        String status = payment.getStatus();
        log.info("Procesando pago MP ID: {} para Venta: {} con estado: {}", paymentId, saleId, status);

        if ("approved".equalsIgnoreCase(status)) {
            sale.setPaymentStatus(PaymentStatus.APROBADO);
            
            if (sale.getSaleStatus() == SaleStatus.PENDIENTE) sale.setSaleStatus(SaleStatus.PREPARACION);
        } else if ("rejected".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
            sale.setPaymentStatus(PaymentStatus.RECHAZADO);
        } else if ("refunded".equalsIgnoreCase(status)) {
            sale.setPaymentStatus(PaymentStatus.REEMBOLSADO);
            sale.setSaleStatus(SaleStatus.REEMBOLSADO);
        }

        this.iSaleRepository.save(sale);
    }
}