package com.proyecto.proyecto_techlab_springboot.sale.controller;

import com.proyecto.proyecto_techlab_springboot.sale.client.MockMercadoPagoClient;
import com.proyecto.proyecto_techlab_springboot.sale.dto.DevMercadoPagoSimulateRequest;
import com.proyecto.proyecto_techlab_springboot.sale.dto.DevMercadoPagoSimulateResponse;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.sale.service.MercadoPagoWebhookService;
import com.proyecto.proyecto_techlab_springboot.sale.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@Profile("dev")
@RequestMapping("/dev/payments/mercadopago")
@RequiredArgsConstructor
public class DevMercadoPagoController {

    private final SaleService saleService;
    private final MockMercadoPagoClient mockMercadoPagoClient;
    private final MercadoPagoWebhookService webhookService;

    @Value("${mercadopago.back-urls.success}")
    private String successUrl;

    @Value("${mercadopago.back-urls.pending}")
    private String pendingUrl;

    @Value("${mercadopago.back-urls.failure}")
    private String failureUrl;

    @PostMapping("/simulate")
    public DevMercadoPagoSimulateResponse simulatePayment(
            @Valid @RequestBody DevMercadoPagoSimulateRequest request
    ) {
        Sale sale = saleService.getSaleByIdOrElseThrow(request.saleId());
        if (sale.getPaymentMethod() != PaymentMethod.MERCADO_PAGO
                || sale.getSaleStatus() != SaleStatus.PENDIENTE
                || (sale.getPaymentStatus() != PaymentStatus.PENDIENTE
                && sale.getPaymentStatus() != PaymentStatus.RECHAZADO)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden simular pagos de Mercado Pago pendientes o rechazados."
            );
        }

        Long paymentId = mockMercadoPagoClient.registerSimulatedPayment(
                sale.getSaleId(),
                request.outcome(),
                sale.getSaleTotal()
        );
        String paymentIdText = paymentId.toString();
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String signature = mockMercadoPagoClient.createWebhookSignature(paymentIdText, timestamp);

        Map<String, Object> payload = Map.of(
                "action", "payment.updated",
                "type", "payment",
                "data", Map.of("id", paymentIdText)
        );
        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        webhookService.processNotification(
                signature,
                (String) payload.get("action"),
                (String) payload.get("type"),
                String.valueOf(data.get("id"))
        );

        Sale updatedSale = saleService.getSaleByIdOrElseThrow(sale.getSaleId());
        String redirectUrl = switch (request.outcome()) {
            case "approved" -> successUrl;
            case "pending" -> pendingUrl;
            case "rejected" -> failureUrl;
            default -> throw new IllegalStateException("Resultado validado inesperadamente.");
        };
        return new DevMercadoPagoSimulateResponse(
                updatedSale.getSaleId(),
                redirectUrl,
                updatedSale.getSaleStatus(),
                updatedSale.getPaymentStatus()
        );
    }
}
