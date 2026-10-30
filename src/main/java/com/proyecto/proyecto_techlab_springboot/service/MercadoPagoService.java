package com.proyecto.proyecto_techlab_springboot.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.proyecto.proyecto_techlab_springboot.config.MercadoPagoConfiguration;
import com.proyecto.proyecto_techlab_springboot.dto.sale.MercadoPagoPreferenceResult;
import com.proyecto.proyecto_techlab_springboot.exception.sale.InvalidWebhookSignatureException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.MercadoPagoIntegrationException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.PaymentNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Sale;
import com.proyecto.proyecto_techlab_springboot.model.SaleItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Creación de Preferencias y Validación Criptográfica
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private final MercadoPagoConfiguration mpConfig;

    /**
     * Crea una preferencia de pago en Mercado Pago (Checkout Pro) con los datos congelados de la venta.
     */
    public MercadoPagoPreferenceResult createPreference(Sale sale) {
        try {
            PreferenceClient            client  = new PreferenceClient();
            List<PreferenceItemRequest> items   = new ArrayList<>();

            // Agregar renglones de prendas
            for (SaleItem saleItem : sale.getItems()) {
                PreferenceItemRequest item = PreferenceItemRequest.builder()
                        .id(String.valueOf(saleItem.getSaleItemId()))
                        .title("%s - %s".formatted(saleItem.getProductName(), saleItem.getVariantDesc()))
                        .description("Talle: %s | Color: %s".formatted(saleItem.getVariantSize(), saleItem.getColorName()))
                        .pictureUrl(saleItem.getImageUrl())
                        .quantity(saleItem.getQuantity())
                        .currencyId("ARS")
                        .unitPrice(saleItem.getUnitPrice())
                        .build();

                items.add(item);
            }

            // Si hubo descuento de rol, agregarlo como ítem de crédito/descuento negativo
            if (sale.getSaleDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                items.add(
                    PreferenceItemRequest.builder()
                        .id("DISCOUNT")
                        .title("Descuento Comercial Aplicado")
                        .quantity(1)
                        .currencyId("ARS")
                        .unitPrice(sale.getSaleDiscountAmount().negate())
                        .build()
                    );

            // Si hubo costo de envío, agregarlo como concepto
            if (sale.getShippingCost().compareTo(BigDecimal.ZERO) > 0) {
                items.add(
                        PreferenceItemRequest.builder()
                                .id("SHIPPING")
                                .title("Costo de Envío")
                                .quantity(1)
                                .currencyId("ARS")
                                .unitPrice(sale.getShippingCost())
                                .build()
                );
            }

            // Datos del pagador
            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .name(sale.getCustomerName())
                    .email(sale.getCustomerEmail())
                    .build();

            // URLs de retorno y notificaciones
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(mpConfig.getBackUrlSuccess())
                    .failure(mpConfig.getBackUrlFailure())
                    .pending(mpConfig.getBackUrlPending())
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .notificationUrl(mpConfig.getNotificationUrl())
                    .externalReference(sale.getSaleId().toString())
                    .expires(true)
                    // Expiración de 48 horas en Checkout Pro
                    .expirationDateTo(OffsetDateTime.now(ZoneOffset.of("-03:00")).plusHours(48))
                    .build();

            Preference preference = client.create(preferenceRequest);

            return new MercadoPagoPreferenceResult(preference.getId(), preference.getInitPoint());
        } catch (MPApiException apiEx) {
            log.error("Error API Mercado Pago [Status: {}]: {}", apiEx.getStatusCode(), apiEx.getApiResponse().getContent());
            throw new MercadoPagoIntegrationException("Fallo en la comunicación con Mercado Pago: " + apiEx.getMessage(), apiEx);
        } catch (MPException ex) {
            log.error("Excepción interna SDK Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoIntegrationException("Error interno al generar preferencia de Mercado Pago.", ex);
        }
    }

    /**
     * Consulta el estado del pago directamente en la API de Mercado Pago.
     */
    public Payment getPaymentDetails(Long paymentId) {
        try {
            return new PaymentClient().get(paymentId);
        } catch (MPApiException apiEx) {
            if (apiEx.getStatusCode() == 404)
                throw new PaymentNotFoundException("No se encontró el pago en Mercado Pago ID: " + paymentId);
            
            throw new MercadoPagoIntegrationException("Error al consultar el pago en Mercado Pago: " + apiEx.getMessage(), apiEx);
        } catch (MPException ex) {
            throw new MercadoPagoIntegrationException("Error SDK al recuperar detalle del pago.", ex);
        }
    }

    /**
     * Valida la firma criptográfica HMAC-SHA256 provista en el header x-signature de Mercado Pago.
     */
    public void validateWebhookSignature(String xSignatureHeader, String dataId) {
        if (xSignatureHeader == null || !xSignatureHeader.contains("ts=") || !xSignatureHeader.contains("v1="))
            throw new InvalidWebhookSignatureException("Encabezado x-signature ausente o con formato no reconocido.");

        try {
            String      ts      = null;
            String      v1      = null;
            String[]    parts   = xSignatureHeader.split(",");

            for (String part : parts) {
                String[] keyValue = part.trim().split("=", 2);

                if (keyValue.length == 2) {
                    if ("ts".equals(keyValue[0])) ts = keyValue[1];
                    if ("v1".equals(keyValue[0])) v1 = keyValue[1];
                }
            }

            if (ts == null || v1 == null)
                throw new InvalidWebhookSignatureException("Faltan elementos ts o v1 en la firma del webhook.");

            // Manifest string según especificación oficial de Mercado Pago
            String manifest = "id:%s;request-id:;ts:%s;".formatted(dataId, ts);

            Mac             hmac        = Mac.getInstance("HmacSHA256");
            SecretKeySpec   secretKey   = new SecretKeySpec(mpConfig.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            if (!MessageDigest.isEqual(hexString.toString().getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8)))
                throw new InvalidWebhookSignatureException("La firma provista no coincide con el secreto configurado.");

        } catch (InvalidWebhookSignatureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidWebhookSignatureException("Fallo en el cálculo de validación de firma: " + ex.getMessage());
        }
    }
}