package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemRequest;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartResponse;
import com.proyecto.proyecto_techlab_springboot.exception.common.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.service.CartService;
import com.proyecto.proyecto_techlab_springboot.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService   cartService;
    private final JwtService    jwtService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(HttpServletRequest request) {
        UUID userId = resolveUserIdFromToken(request);
        return ResponseEntity.ok(cartService.getCartByUser(userId));
    }

    @PostMapping("/item")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody CartItemRequest itemRequest,
            HttpServletRequest request
    ) {
        UUID userId = resolveUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(userId, itemRequest));
    }

    @PatchMapping("/item/{variantId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable       Long                    variantId,
            @Valid @RequestBody CartItemUpdateRequest   updateRequest,
            HttpServletRequest request
    ) {
        UUID userId = resolveUserIdFromToken(request);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, variantId, updateRequest));
    }

    @DeleteMapping("/item/{variantId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long variantId,
            HttpServletRequest request
    ) {
        UUID userId = resolveUserIdFromToken(request);
        cartService.removeItemFromCart(userId, variantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(HttpServletRequest request) {
        UUID userId = resolveUserIdFromToken(request);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extrae el userId validando la firma del JWT directamente,
     * prescindiendo de Spring Security mientras esté desactivado.
     */
    private UUID resolveUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new InvalidCredentialsException("Encabezado de autorización ausente o con formato inválido.");

        String token = authHeader.substring(7);
        if (!jwtService.validateTokenStructure(token))
            throw new InvalidCredentialsException("El token provisto es inválido o se encuentra expirado.");

        String userIdClaim = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
        if (userIdClaim == null)
            throw new InvalidCredentialsException("El token no contiene el claim de usuario requerido.");

        return UUID.fromString(userIdClaim);
    }
}