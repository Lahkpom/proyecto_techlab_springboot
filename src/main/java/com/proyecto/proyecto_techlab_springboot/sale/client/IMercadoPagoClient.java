package com.proyecto.proyecto_techlab_springboot.sale.client;

import com.proyecto.proyecto_techlab_springboot.sale.dto.MercadoPagoPreferenceResult;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;

import java.math.BigDecimal;
import java.util.UUID;

public interface IMercadoPagoClient {

    MercadoPagoPreferenceResult createPreference(Sale sale);

    MercadoPagoPaymentDetails getPaymentDetails(Long paymentId);

    void validateWebhookSignature(String xSignatureHeader, String dataId);

    Long refundPayment(Long mpPaymentId, BigDecimal amountToRefund, UUID returnId);

    record MercadoPagoPaymentDetails(
            Long paymentId,
            String status,
            String externalReference,
            BigDecimal transactionAmount
    ) {}
}