package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.service.MercadoPagoWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments/mercadopago")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final MercadoPagoWebhookService webhookService;

    /**
     * Endpoint público para recepción de Webhooks de Mercado Pago.
     * Retorna siempre 200 OK tras procesar para evitar reintentos continuos de la pasarela.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String paramId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String action = null;
        String dataId = paramId;

        if (body != null) {
            action = (String) body.get("action");
            if (topic == null) topic = (String) body.get("type");
            
            if (body.get("data") instanceof Map<?, ?> dataMap) {
                Object idObj = dataMap.get("id");
                
                if (idObj != null) dataId = String.valueOf(idObj);
            }
        }

        webhookService.processNotification(xSignature, action, topic, dataId);
        return ResponseEntity.ok().build();
    }
}