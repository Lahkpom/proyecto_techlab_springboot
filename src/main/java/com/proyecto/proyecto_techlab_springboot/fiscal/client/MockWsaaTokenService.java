package com.proyecto.proyecto_techlab_springboot.fiscal.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Component
@Profile("dev")
public class MockWsaaTokenService implements IWsaaTokenClient {

    private AccessTicket cachedTicket;

    @Override
    public synchronized AccessTicket getAccessTicket() {
        if (cachedTicket != null && cachedTicket.isValid()) return cachedTicket;

        log.info("[ARCA MOCK - DEV] Generando Ticket de Acceso simulado.");
        this.cachedTicket = AccessTicket.builder()
                .token("MOCK_TOKEN_ARCA_" + UUID.randomUUID())
                .sign("MOCK_SIGN_ARCA_" + UUID.randomUUID())
                .expiration(Instant.now().plus(12, ChronoUnit.HOURS))
                .build();
        
        return this.cachedTicket;
    }
}