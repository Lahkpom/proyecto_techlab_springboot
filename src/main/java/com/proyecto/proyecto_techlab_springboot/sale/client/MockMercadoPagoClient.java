package com.proyecto.proyecto_techlab_springboot.sale.client;

import com.proyecto.proyecto_techlab_springboot.sale.dto.MercadoPagoPreferenceResult;
import com.proyecto.proyecto_techlab_springboot.sale.exception.PaymentNotFoundException;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.sale.repository.ISaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Como SaleService inyecta IMercadoPagoClient, MockMercadoPagoClient consulta ISaleRepository directamente para resolver el último pedido 
 * pendiente en getPaymentDetails, evitando así una dependencia circular SaleService $\leftrightarrow$ MockMercadoPagoClient).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MockMercadoPagoClient implements IMercadoPagoClient {

    private final ISaleRepository iSaleRepository;

    @Override
    public MercadoPagoPreferenceResult createPreference(Sale sale) {
        String mockPrefId   = "MOCK-PREF-" + UUID.randomUUID();
        String mockInitUrl  = "https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=" + mockPrefId;

        log.info("[MP MOCK - DEV] Preferencia simulada generada para Venta ID {}: { prefId: {} }", sale.getSaleId(), mockPrefId);
        return new MercadoPagoPreferenceResult(mockPrefId, mockInitUrl);
    }

    @Override
    public MercadoPagoPaymentDetails getPaymentDetails(Long paymentId) {
        log.info("[MP MOCK - DEV] Consultando detalles simulados para Payment ID: {}", paymentId);

        // Busca la orden pendiente de Mercado Pago más reciente para vincular el webhook de prueba automáticamente
        Sale targetSale = this.iSaleRepository
                .findFirstByPaymentMethodAndPaymentStatusOrderByCreatedAtDesc(PaymentMethod.MERCADO_PAGO, PaymentStatus.PENDIENTE)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "[MP MOCK - DEV] No hay ninguna orden en estado PENDIENTE con método MERCADO_PAGO para asociar el Payment ID: " + paymentId
                ));

        return new MercadoPagoPaymentDetails(
                paymentId,
                "approved",
                targetSale.getSaleId().toString(),
                targetSale.getSaleTotal()
        );
    }

    @Override
    public void validateWebhookSignature(String xSignatureHeader, String dataId) {
        log.info("[MP MOCK - DEV] Omitiendo validación criptográfica HMAC-SHA256 para dataId: {}", dataId);
    }

    @Override
    public Long refundPayment(Long mpPaymentId, BigDecimal amountToRefund, UUID returnId) {
        long mockRefundId = ThreadLocalRandom.current().nextLong(100000000L, 999999999L);
        log.info("[MP MOCK - DEV] Reembolso simulado ejecutado { mockRefundId: {}, paymentId: {}, monto: ${}, returnId: {} }",
                mockRefundId, mpPaymentId, amountToRefund, returnId);
        return mockRefundId;
    }
}