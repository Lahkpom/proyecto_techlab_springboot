package com.proyecto.proyecto_techlab_springboot.sale.client;

import com.proyecto.proyecto_techlab_springboot.sale.dto.MercadoPagoPreferenceResult;
import com.proyecto.proyecto_techlab_springboot.sale.exception.InvalidWebhookSignatureException;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockMercadoPagoClientTest {

    private static final String WEBHOOK_SECRET = "dev-webhook-secret";

    private final MockMercadoPagoClient client =
            new MockMercadoPagoClient("http://localhost:5173/checkout/mp-sandbox", WEBHOOK_SECRET);

    @Test
    void createPreferenceReturnsFrontendSandboxWithSaleContext() {
        UUID saleId = UUID.randomUUID();
        Sale sale = Sale.builder()
                .saleId(saleId)
                .saleTotal(new BigDecimal("1250.50"))
                .build();

        MercadoPagoPreferenceResult preference = client.createPreference(sale);

        assertTrue(preference.preferenceId().startsWith("MOCK-PREF-"));
        assertEquals(
                "http://localhost:5173/checkout/mp-sandbox?saleId=%s&preferenceId=%s&amount=1250.50"
                        .formatted(saleId, preference.preferenceId()),
                preference.initPoint()
        );
    }

    @Test
    void simulatedPaymentDetailsAreKeyedByUniquePaymentId() {
        UUID firstSaleId = UUID.randomUUID();
        UUID secondSaleId = UUID.randomUUID();
        Long firstPaymentId = client.registerSimulatedPayment(
                firstSaleId,
                "approved",
                new BigDecimal("100.00")
        );
        Long secondPaymentId = client.registerSimulatedPayment(
                secondSaleId,
                "rejected",
                new BigDecimal("250.75")
        );

        assertNotEquals(firstPaymentId, secondPaymentId);
        assertEquals(firstSaleId.toString(), client.getPaymentDetails(firstPaymentId).externalReference());
        assertEquals("approved", client.getPaymentDetails(firstPaymentId).status());
        assertEquals(new BigDecimal("100.00"), client.getPaymentDetails(firstPaymentId).transactionAmount());
        assertEquals(secondSaleId.toString(), client.getPaymentDetails(secondPaymentId).externalReference());
        assertEquals("rejected", client.getPaymentDetails(secondPaymentId).status());
        assertEquals(new BigDecimal("250.75"), client.getPaymentDetails(secondPaymentId).transactionAmount());
    }

    @Test
    void webhookSignatureMatchesAndRejectsTampering() {
        String timestamp = "1795460000";
        String signature = client.createWebhookSignature("12345", timestamp);

        client.validateWebhookSignature(signature, "12345");
        assertThrows(
                InvalidWebhookSignatureException.class,
                () -> client.validateWebhookSignature(signature, "54321")
        );
    }
}
