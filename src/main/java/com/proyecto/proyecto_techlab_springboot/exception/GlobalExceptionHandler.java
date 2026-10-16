package com.proyecto.proyecto_techlab_springboot.exception;

import com.proyecto.proyecto_techlab_springboot.exception.auth.AccountLockedException;
import com.proyecto.proyecto_techlab_springboot.exception.auth.EmailNotVerifiedException;
import com.proyecto.proyecto_techlab_springboot.exception.auth.ExpiredPasswordException;
import com.proyecto.proyecto_techlab_springboot.exception.common.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.exception.common.ErrorResponse;
import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice // Escucha las excepciones de TODOS los @RestController
public class GlobalExceptionHandler {

    // Maneja 400 (validaciones de @Valid (@NotBlank, @Pattern, @Size))
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Datos de entrada inválidos.";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                detail,
                request
        );
    }

    // Maneja 400 (cuando mandan letras en un ID numérico o un UUID roto)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detail = "El parámetro '%s' con valor '%s' no tiene el formato esperado.".formatted(ex.getName(), ex.getValue());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                detail,
                request
        );
    }

    // Maneja 401 (Credenciales inválidas en login)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                ex.getMessage(),
                request
        );
    }

    // Maneja 403 (Email pendiente de verificación)
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "EMAIL_NOT_VERIFIED",
                ex.getMessage(),
                request
        );
    }

    // Maneja 403 (Contraseña expirada)
    @ExceptionHandler(ExpiredPasswordException.class)
    public ResponseEntity<ErrorResponse> handleExpiredPassword(ExpiredPasswordException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "PASSWORD_EXPIRED",
                ex.getMessage(),
                request
        );
    }

    // Maneja TODOS los 404 de cualquier entidad (Product, Color, User, etc.)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request
        );
    }

    // Maneja TODOS los 409 (Conflictos / Duplicados) de cualquier entidad (Product, Color, User, etc.)
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(ResourceAlreadyExistsException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "RESOURCE_ALREADY_EXISTS",
                ex.getMessage(),
                request
        );
    }

    // Maneja 423 (Cuenta bloqueada por fuerza bruta)
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.LOCKED,
                "ACCOUNT_LOCKED",
                ex.getMessage(),
                request
        );
    }

    // Maneja 500 (Ataja cualquier error inesperado)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado al procesar {} {}: ", request.getMethod(), request.getRequestURI(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Ocurrió un error inesperado. Contacte al administrador si el problema persiste.",
                request
        );
    }

    // Helper privado para no repetir la estructura del JSON
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus          status,
            String              error,
            String              detail,
            HttpServletRequest  request
    ) {
        return ResponseEntity
                .status(status)
                .body(
                        ErrorResponse.of(
                                status.value(),
                                error,
                                detail,
                                request.getRequestURI()
                        )
                );
    }
}