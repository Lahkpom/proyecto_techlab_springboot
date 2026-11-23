package com.proyecto.proyecto_techlab_springboot.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Inicializa MercadoPagoConfig.setAccessToken() al iniciar el contenedor de Spring Boot.
 */
@Configuration
@Profile("prod")
@Getter
public class MercadoPagoConfiguration {

    private final String accessToken;
    private final String webhookSecret;
    private final String notificationUrl;
    private final String backUrlSuccess;
    private final String backUrlFailure;
    private final String backUrlPending;

    public MercadoPagoConfiguration(
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.webhook-secret}") String webhookSecret,
            @Value("${mercadopago.notification-url}") String notificationUrl,
            @Value("${mercadopago.back-urls.success}") String backUrlSuccess,
            @Value("${mercadopago.back-urls.failure}") String backUrlFailure,
            @Value("${mercadopago.back-urls.pending}") String backUrlPending
    ) {
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
        this.notificationUrl = notificationUrl;
        this.backUrlSuccess = backUrlSuccess;
        this.backUrlFailure = backUrlFailure;
        this.backUrlPending = backUrlPending;
    }

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}