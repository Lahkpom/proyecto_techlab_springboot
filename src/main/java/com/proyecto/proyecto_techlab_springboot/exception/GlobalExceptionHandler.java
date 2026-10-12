package com.proyecto.proyecto_techlab_springboot.exception;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice // Escucha las excepciones de TODOS los @RestController
public class GlobalExceptionHandler {

    // Maneja validaciones de @Valid (@NotBlank, @Pattern, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Datos inválidos";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Error de validación",
                detail
        );
    }

    // Maneja cuando mandan letras en un ID numérico o un UUID roto
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "El parámetro '%s' con valor '%s' no tiene el formato esperado.".formatted(ex.getName(), ex.getValue());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Tipo de parámetro inválido",
                detail
        );
    }

    // Maneja TODOS los 404 de cualquier entidad (Product, Color, User, etc.)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Recurso no encontrado.",
                ex.getMessage()
        );
    }

    // Maneja TODOS los 409 (Duplicados) de cualquier entidad (Product, Color, User, etc.)
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Conflicto de duplicidad",
                ex.getMessage()
        );
    }

    // Helper privado para no repetir la estructura del JSON
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String detail) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp" , Instant.now(),
                "status"    , status.value(),
                "error"     , error,
                "detail"    , detail
        ));
    }
}