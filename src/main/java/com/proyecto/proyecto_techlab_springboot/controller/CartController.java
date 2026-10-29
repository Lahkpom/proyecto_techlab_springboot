package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemRequest;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartResponse;
import com.proyecto.proyecto_techlab_springboot.service.CartService;
import com.proyecto.proyecto_techlab_springboot.util.SecurityContextUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService           cartService;    
    private final SecurityContextUtil   securityContextUtil;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(HttpServletRequest request) {
        UUID userId = this.securityContextUtil.resolveRequiredUserIdFromToken(request);
        return ResponseEntity.ok(cartService.getCartByUser(userId));
    }

    @PostMapping("/item")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody CartItemRequest itemRequest,
            HttpServletRequest request
    ) {
        UUID userId = this.securityContextUtil.resolveRequiredUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(userId, itemRequest));
    }

    @PatchMapping("/item/{variantId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable       Long                    variantId,
            @Valid @RequestBody CartItemUpdateRequest   updateRequest,
            HttpServletRequest request
    ) {
        UUID userId = this.securityContextUtil.resolveRequiredUserIdFromToken(request);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, variantId, updateRequest));
    }

    @DeleteMapping("/item/{variantId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long variantId,
            HttpServletRequest request
    ) {
        UUID userId = this.securityContextUtil.resolveRequiredUserIdFromToken(request);
        cartService.removeItemFromCart(userId, variantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(HttpServletRequest request) {
        UUID userId = this.securityContextUtil.resolveRequiredUserIdFromToken(request);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}