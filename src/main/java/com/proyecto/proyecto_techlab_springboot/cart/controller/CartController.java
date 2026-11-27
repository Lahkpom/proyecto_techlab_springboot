package com.proyecto.proyecto_techlab_springboot.cart.controller;

import com.proyecto.proyecto_techlab_springboot.cart.dto.CartItemRequest;
import com.proyecto.proyecto_techlab_springboot.cart.dto.CartItemUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.cart.dto.CartResponse;
import com.proyecto.proyecto_techlab_springboot.cart.dto.CartSyncRequest;
import com.proyecto.proyecto_techlab_springboot.cart.service.CartService;
import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ResponseEntity.ok(cartService.getCartByUser(principal.userId()));
    }

    @PostMapping("/item")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody CartItemRequest itemRequest,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(principal.userId(), itemRequest));
    }

    @PostMapping("/sync")
    public ResponseEntity<CartResponse> syncAnonymousCart(@Valid @RequestBody CartSyncRequest request) {
        return ResponseEntity.ok(cartService.syncAnonymousCart(request.items()));
    }

    @PatchMapping("/item/{variantId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable       Long                    variantId,
            @Valid @RequestBody CartItemUpdateRequest   updateRequest,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(cartService.updateItemQuantity(principal.userId(), variantId, updateRequest));
    }

    @DeleteMapping("/item/{variantId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long variantId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        cartService.removeItemFromCart(principal.userId(), variantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        cartService.clearCart(principal.userId());
        return ResponseEntity.noContent().build();
    }
}