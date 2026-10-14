package com.proyecto.proyecto_techlab_springboot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final JdbcClient jdbcClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(
            UUID    userId,
            String  attemptedEmail,
            boolean isSuccess,
            String  failureReason,
            String  ipAddress,
            String  userAgent
    ) {
        try {
            jdbcClient.sql("""
                INSERT INTO user_login_log (
                    user_id,
                    attempted_email,
                    is_success,
                    failure_reason,
                    ip_address,
                    user_agent
                ) VALUES (
                    :userId,
                    :attemptedEmail,
                    :isSuccess,
                    :failureReason,
                    cast(:ipAddress as inet),
                    :userAgent
                )
            """)
                    .param("userId"         , userId)
                    .param("attemptedEmail" , attemptedEmail)
                    .param("isSuccess"      , isSuccess)
                    .param("failureReason"  , failureReason)
                    .param("ipAddress"      , ipAddress)
                    .param("userAgent"      , userAgent)
                    .update();
        } catch (Exception ex) {
            // El fallo de auditoría no debe tirar abajo el backend, solo advertir
            log.error("No se pudo registrar el intento de login para {}: {}", attemptedEmail, ex.getMessage());
        }
    }
}