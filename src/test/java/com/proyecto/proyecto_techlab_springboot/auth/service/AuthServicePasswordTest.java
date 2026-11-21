package com.proyecto.proyecto_techlab_springboot.auth.service;

import com.proyecto.proyecto_techlab_springboot.auth.dto.ExpiredPasswordResetRequest;
import com.proyecto.proyecto_techlab_springboot.auth.dto.LoginRequest;
import com.proyecto.proyecto_techlab_springboot.auth.dto.OperatorCreateRequest;
import com.proyecto.proyecto_techlab_springboot.auth.dto.WholesalerCreateRequest;
import com.proyecto.proyecto_techlab_springboot.auth.exception.ExpiredPasswordException;
import com.proyecto.proyecto_techlab_springboot.auth.model.UserSession;
import com.proyecto.proyecto_techlab_springboot.auth.repository.IUserSessionRepository;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.isNull;

class AuthServicePasswordTest {

    @Test
    void createsOperatorWithConfiguredInitialPasswordAndForcedChange() {
        UserService userService = mock(UserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService service = service(userService, passwordEncoder, mock(IUserSessionRepository.class));
        ReflectionTestUtils.setField(service, "initialPassword", "configured-initial-password");
        when(passwordEncoder.encode("configured-initial-password")).thenReturn("encoded-initial-password");
        when(userService.createAccount(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createOperator(
                new OperatorCreateRequest("Operador", "operator@example.com", "1234567890")
        );

        assertEquals(UserRole.OPERATOR, response.userRole());
        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        verify(userService).createAccount(userCaptor.capture());
        Users savedUser = userCaptor.getValue();
        assertEquals("encoded-initial-password", savedUser.getUserPassword());
        assertTrue(savedUser.isMustChangePassword());
        assertNotNull(savedUser.getPasswordExpiresAt());
        assertFalse(savedUser.getPasswordExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void createsWholesalerWithInitialPasswordWhenPasswordIsOmitted() {
        UserService userService = mock(UserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService service = service(userService, passwordEncoder, mock(IUserSessionRepository.class));
        ReflectionTestUtils.setField(service, "initialPassword", "configured-initial-password");
        when(passwordEncoder.encode("configured-initial-password")).thenReturn("encoded-initial-password");
        when(userService.createAccount(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createWholesaler(new WholesalerCreateRequest(
                "Mayorista",
                "wholesaler@example.com",
                null,
                null,
                "Compañía",
                TaxIdType.CUIT,
                "20123456789"
        ));

        var userCaptor = org.mockito.ArgumentCaptor.forClass(Users.class);
        verify(userService).createAccount(userCaptor.capture());
        Users savedUser = userCaptor.getValue();
        assertEquals(UserRole.WHOLESALER, savedUser.getUserRole());
        assertEquals("encoded-initial-password", savedUser.getUserPassword());
        assertTrue(savedUser.isMustChangePassword());
        assertFalse(savedUser.getPasswordExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void loginAuditsAndRejectsAccountsMarkedForForcedPasswordChange() {
        UserService userService = mock(UserService.class);
        LoginAuditService auditService = mock(LoginAuditService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UUID userId = UUID.randomUUID();
        Users user = user(userId, "operator@example.com", UserRole.OPERATOR, true);
        when(userService.findUserByEmail("operator@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("initial-password", "encoded-password")).thenReturn(true);
        AuthService service = service(
                userService,
                passwordEncoder,
                mock(IUserSessionRepository.class),
                mock(JwtService.class),
                auditService
        );
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        assertThrows(
                ExpiredPasswordException.class,
                () -> service.login(new LoginRequest("operator@example.com", "initial-password"), httpRequest)
        );

        verify(auditService).recordAttempt(
                eq(userId),
                eq("operator@example.com"),
                eq(false),
                eq("PASSWORD_EXPIRED"),
                isNull(),
                isNull()
        );
    }

    @Test
    void expiredPasswordChangeUpdatesCredentialsAndStartsSession() {
        UserService userService = mock(UserService.class);
        IUserSessionRepository sessionRepository = mock(IUserSessionRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        LoginAuditService auditService = mock(LoginAuditService.class);
        UUID userId = UUID.randomUUID();
        Users user = user(userId, "operator@example.com", UserRole.OPERATOR, true);
        when(userService.findUserByEmail("operator@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("initial-password", "encoded-password")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "encoded-password")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(sessionRepository.save(any(UserSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AuthService service = service(
                userService,
                passwordEncoder,
                sessionRepository,
                jwtService,
                auditService
        );
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("test-agent");

        var response = service.changeExpiredPassword(
                new ExpiredPasswordResetRequest(
                        "operator@example.com",
                        "initial-password",
                        "new-password"
                ),
                httpRequest
        );

        assertEquals("access-token", response.accessToken());
        assertEquals("encoded-new-password", user.getUserPassword());
        assertFalse(user.isMustChangePassword());
        assertTrue(user.getPasswordExpiresAt().isAfter(Instant.now()));
        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertEquals("127.0.0.1", sessionCaptor.getValue().getIpAddress());
        verify(auditService).recordAttempt(
                eq(userId),
                eq("operator@example.com"),
                eq(true),
                isNull(),
                eq("127.0.0.1"),
                eq("test-agent")
        );
    }

    private AuthService service(
            UserService userService,
            PasswordEncoder passwordEncoder,
            IUserSessionRepository sessionRepository
    ) {
        return service(
                userService,
                passwordEncoder,
                sessionRepository,
                mock(JwtService.class),
                mock(LoginAuditService.class)
        );
    }

    private AuthService service(
            UserService userService,
            PasswordEncoder passwordEncoder,
            IUserSessionRepository sessionRepository,
            JwtService jwtService,
            LoginAuditService auditService
    ) {
        return new AuthService(sessionRepository, userService, passwordEncoder, jwtService, auditService);
    }

    private Users user(UUID userId, String email, UserRole role, boolean mustChangePassword) {
        return Users.builder()
                .userId(userId)
                .userRole(role)
                .userName("Test User")
                .userEmail(email)
                .userPassword("encoded-password")
                .isEmailVerified(true)
                .mustChangePassword(mustChangePassword)
                .passwordExpiresAt(Instant.now())
                .rowStatus(RowStatus.ACTIVE)
                .build();
    }
}
