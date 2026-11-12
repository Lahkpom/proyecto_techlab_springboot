package com.proyecto.proyecto_techlab_springboot.fiscal.client;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public interface IWsaaTokenClient {

    AccessTicket getAccessTicket();

    @Getter
    @Builder
    class AccessTicket {
        private String  token;
        private String  sign;
        private Instant expiration;

        public boolean isValid() {
            return expiration != null && Instant.now().plus(10, ChronoUnit.MINUTES).isBefore(expiration);
        }
    }
}