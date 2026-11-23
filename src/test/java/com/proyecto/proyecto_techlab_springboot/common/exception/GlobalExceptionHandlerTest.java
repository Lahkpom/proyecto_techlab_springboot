package com.proyecto.proyecto_techlab_springboot.common.exception;

import com.proyecto.proyecto_techlab_springboot.sale.exception.MercadoPagoIntegrationException;
import com.proyecto.proyecto_techlab_springboot.salereturn.exception.InvalidReturnItemException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorsReturnSafeGenericDetail() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("database password leaked"),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                "Ocurrió un error interno inesperado. Por favor, intentá nuevamente más tarde.",
                response.getBody().detail()
        );
        assertFalse(response.getBody().detail().contains("database password"));
    }

    @Test
    void invalidReturnItemMapsToUnprocessableContent() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidReturnItem(
                new InvalidReturnItemException("El ítem no pertenece a la venta."),
                request()
        );

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertEquals("INVALID_RETURN_ITEM", response.getBody().error());
    }

    @Test
    void mercadoPagoErrorsReturnSafeDetail() {
        ResponseEntity<ErrorResponse> response = handler.handleMercadoPagoIntegration(
                new MercadoPagoIntegrationException("access_token=secret response={\"private\":\"data\"}"),
                request()
        );

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertFalse(response.getBody().detail().contains("secret"));
        assertFalse(response.getBody().detail().contains("private"));
    }

    private HttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/test");
        return request;
    }
}
