package com.proyecto.proyecto_techlab_springboot.sale.client;

import com.proyecto.proyecto_techlab_springboot.sale.dto.MercadoPagoPreferenceResult;
import com.proyecto.proyecto_techlab_springboot.sale.exception.InvalidWebhookSignatureException;
import com.proyecto.proyecto_techlab_springboot.sale.exception.PaymentNotFoundException;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Como SaleService inyecta IMercadoPagoClient, MockMercadoPagoClient consulta ISaleRepository directamente para resolver el último pedido 
 * pendiente en getPaymentDetails, evitando así una dependencia circular SaleService $\leftrightarrow$ MockMercadoPagoClient).
 */
@Slf4j
@Component
@Profile("dev")
public class MockMercadoPagoClient implements IMercadoPagoClient {

    private final String sandboxUrl;
    private final String webhookSecret;
    private final ConcurrentMap<Long, SimulatedPaymentData> simulatedPayments = new ConcurrentHashMap<>();
    private final AtomicLong paymentIdSequence = new AtomicLong(System.currentTimeMillis());

    public MockMercadoPagoClient(
            @Value("${mercadopago.sandbox-url}") String sandboxUrl,
            @Value("${mercadopago.webhook-secret}") String webhookSecret
    ) {
        this.sandboxUrl = sandboxUrl;
        this.webhookSecret = webhookSecret;
    }

    public record SimulatedPaymentData(UUID saleId, String status, BigDecimal amount) {}

    @Override
    public MercadoPagoPreferenceResult createPreference(Sale sale) {
        String mockPrefId = "MOCK-PREF-%s".formatted(UUID.randomUUID());
        String mockInitUrl = UriComponentsBuilder.fromUriString(sandboxUrl)
                .queryParam("saleId", sale.getSaleId())
                .queryParam("preferenceId", mockPrefId)
                .queryParam("amount", sale.getSaleTotal().toPlainString())
                .build()
                .encode()
                .toUriString();

        log.info("[MP MOCK - DEV] Preferencia simulada generada para Venta ID {}: { prefId: {} }", sale.getSaleId(), mockPrefId);
        return new MercadoPagoPreferenceResult(mockPrefId, mockInitUrl);
    }

    public Long registerSimulatedPayment(UUID saleId, String outcome, BigDecimal amount) {
        if (!"approved".equals(outcome) && !"pending".equals(outcome) && !"rejected".equals(outcome)) {
            throw new IllegalArgumentException("Resultado de pago simulado no admitido: %s".formatted(outcome));
        }

        long paymentId = paymentIdSequence.incrementAndGet();
        simulatedPayments.put(paymentId, new SimulatedPaymentData(saleId, outcome, amount));
        return paymentId;
    }

    public String createWebhookSignature(String dataId, String timestamp) {
        try {
            String manifest = "id:%s;request-id:;ts:%s;".formatted(dataId, timestamp);
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] hash = hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte value : hash) {
                hexString.append(String.format("%02x", value));
            }
            return "ts=%s,v1=%s".formatted(timestamp, hexString);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar el webhook simulado.", exception);
        }
    }

    @Override
    public MercadoPagoPaymentDetails getPaymentDetails(Long paymentId) {
        SimulatedPaymentData payment = simulatedPayments.get(paymentId);
        if (payment == null) {
            throw new PaymentNotFoundException(
                    "[MP MOCK - DEV] No existe un pago simulado para Payment ID: %s".formatted(paymentId)
            );
        }

        log.info("[MP MOCK - DEV] Consultando detalles simulados para Payment ID: {}", paymentId);
        return new MercadoPagoPaymentDetails(
                paymentId,
                payment.status(),
                payment.saleId().toString(),
                payment.amount()
        );
    }

    @Override
    public void validateWebhookSignature(String xSignatureHeader, String dataId) {
        if (xSignatureHeader == null || !xSignatureHeader.contains("ts=") || !xSignatureHeader.contains("v1=")) {
            throw new InvalidWebhookSignatureException("Encabezado x-signature ausente o con formato no reconocido.");
        }

        try {
            String timestamp = null;
            String signature = null;
            for (String part : xSignatureHeader.split(",")) {
                String[] keyValue = part.trim().split("=", 2);
                if (keyValue.length == 2) {
                    if ("ts".equals(keyValue[0])) timestamp = keyValue[1];
                    if ("v1".equals(keyValue[0])) signature = keyValue[1];
                }
            }

            if (timestamp == null || signature == null) {
                throw new InvalidWebhookSignatureException("Faltan elementos ts o v1 en la firma del webhook.");
            }

            String manifest = "id:%s;request-id:;ts:%s;".formatted(dataId, timestamp);
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder expectedSignature = new StringBuilder();
            for (byte value : hash) {
                expectedSignature.append(String.format("%02x", value));
            }

            if (!MessageDigest.isEqual(
                    expectedSignature.toString().getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            )) {
                throw new InvalidWebhookSignatureException("La firma provista no coincide con el secreto configurado.");
            }
        } catch (InvalidWebhookSignatureException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidWebhookSignatureException(
                    "Fallo en el cálculo de validación de firma: %s".formatted(exception.getMessage())
            );
        }
    }

    @Override
    public Long refundPayment(Long mpPaymentId, BigDecimal amountToRefund, UUID returnId) {
        long mockRefundId = ThreadLocalRandom.current().nextLong(100000000L, 999999999L);
        log.info("[MP MOCK - DEV] Reembolso simulado ejecutado { mockRefundId: {}, paymentId: {}, monto: ${}, returnId: {} }",
                mockRefundId, mpPaymentId, amountToRefund, returnId);
        return mockRefundId;
    }
}