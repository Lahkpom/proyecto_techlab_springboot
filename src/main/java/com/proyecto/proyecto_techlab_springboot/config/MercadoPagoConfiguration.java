package com.proyecto.proyecto_techlab_springboot.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Inicializa MercadoPagoConfig.setAccessToken() al iniciar el contenedor de Spring Boot.
 */
@Configuration
@Getter
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Value("${mercadopago.back-urls.success}")
    private String backUrlSuccess;

    @Value("${mercadopago.back-urls.failure}")
    private String backUrlFailure;

    @Value("${mercadopago.back-urls.pending}")
    private String backUrlPending;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}