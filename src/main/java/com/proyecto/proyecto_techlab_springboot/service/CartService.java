package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemRequest;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemResponse;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartResponse;
import com.proyecto.proyecto_techlab_springboot.exception.cart.CartItemNotFoundException;
import com.proyecto.proyecto_techlab_springboot.exception.variant.VariantNoLongerAvailableException;
import com.proyecto.proyecto_techlab_springboot.exception.variant.VariantNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.exception.cart.InsufficientStockException;
import com.proyecto.proyecto_techlab_springboot.model.CartItem;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.model.CartItemId;
import com.proyecto.proyecto_techlab_springboot.repository.ICartItemRepository;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import com.proyecto.proyecto_techlab_springboot.repository.IVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final ICartItemRepository   iCartItemRepository;
    private final IUserRepository       iUserRepository;
    private final IVariantRepository    iVariantRepository;

    private final UserService       userService;
    private final VariantService       variantService;

    @Transactional
    public CartResponse getCartByUser(UUID userId) {
        this.userService.findUserByIdOrThrow(userId);
        return fetchAndSyncCartResponse(userId);
    }

    @Transactional
    public CartResponse addItemToCart(UUID userId, CartItemRequest request) {
        Users user = this.userService.findUserByIdOrThrow(userId);

        Variant variant = this.variantService.findVariantByIdOrThrow(request.variantId());

        if (variant.getRowStatus() != RowStatus.ACTIVE)
            throw new VariantNoLongerAvailableException("{ ID: %s }".formatted(request.variantId()));

        CartItemId  cartItemId  = new CartItemId(userId, variant.getVariantId());
        CartItem    cartItem    = this.iCartItemRepository.findById(cartItemId).orElse(null);

        int targetQuantity = request.quantity();

        if (cartItem != null) {
            // Ya existía: sumar la cantidad
            targetQuantity = cartItem.getQuantity() + request.quantity();
            if (targetQuantity > variant.getVariantStock()) {
                throw new InsufficientStockException(
                        String.format("Stock insuficiente. Solicitado total: %d, disponible: %d",
                                targetQuantity, variant.getVariantStock())
                );
            }
            cartItem.setQuantity(targetQuantity);
        } else {
            // No existía: validar stock inicial e insertar
            if (targetQuantity > variant.getVariantStock()) {
                throw new InsufficientStockException(
                        String.format("Stock insuficiente. Solicitado: %d, disponible: %d",
                                targetQuantity, variant.getVariantStock())
                );
            }
            cartItem = new CartItem(user, variant, targetQuantity);
        }

        iCartItemRepository.save(cartItem);

        // Devolver el carrito completo y actualizado
        return fetchAndSyncCartResponse(userId);
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID userId, Long variantId, CartItemUpdateRequest request) {
        this.userService.findUserByIdOrThrow(userId);

        CartItemId cartItemId = new CartItemId(userId, variantId);

        CartItem cartItem = iCartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException("{ ID: %s }".formatted(variantId)));

        Variant variant = cartItem.getVariant();
        if (variant.getRowStatus() != RowStatus.ACTIVE) {
            iCartItemRepository.delete(cartItem);
            throw new VariantNoLongerAvailableException("{ ID: %s }".formatted(variant.getVariantId()));
        }

        if (request.quantity() > variant.getVariantStock())
            throw new InsufficientStockException(
                    "No es posible asignar %d unidades. Stock disponible: %d"
                            .formatted(
                                    request.quantity(),
                                    variant.getVariantStock()
                            )
            );

        cartItem.setQuantity(request.quantity());
        iCartItemRepository.save(cartItem);

        return fetchAndSyncCartResponse(userId);
    }

    @Transactional
    public void removeItemFromCart(UUID userId, Long variantId) {
        int deleted = iCartItemRepository.deleteByUserIdAndVariantId(userId, variantId);

        if (deleted == 0)
            throw new VariantNotFoundException(
                    "No exite un registro para el usuario que realiza la consulta donde en su carrito contenga la variante { ID: %s }"
                            .formatted(variantId)
            );
    }

    @Transactional
    public void clearCart(UUID userId) {
        this.userService.findUserByIdOrThrow(userId);
        iCartItemRepository.deleteAllByUserId(userId);
    }

    private CartResponse fetchAndSyncCartResponse(UUID userId) {
        List<CartItem> storedItems = this.iCartItemRepository.findAllByUserIdWithDetails(userId);

        List<CartItem> itemsToKeep = new ArrayList<>();
        List<CartItem> itemsToDelete = new ArrayList<>();
        boolean itemsAdjusted = false;

        for (CartItem item : storedItems) {
            Variant variant = item.getVariant();

            if (variant.getRowStatus() != RowStatus.ACTIVE) {
                itemsToDelete.add(item);
                continue;
            }

            int currentStock = variant.getVariantStock();
            if (currentStock <= 0) {
                itemsToDelete.add(item);
                continue;
            }

            if (item.getQuantity() > currentStock) {
                item.setQuantity(currentStock);
                itemsAdjusted = true;
            }

            itemsToKeep.add(item);
        }

        if (!itemsToDelete.isEmpty()) {
            this.iCartItemRepository.deleteAllInBatch(itemsToDelete);
            log.info("Se purgaron {} ítems obsoletos del carrito del usuario {}", itemsToDelete.size(), userId);
        }

        return buildCartResponse(userId, itemsToKeep, !itemsToDelete.isEmpty(), itemsAdjusted);
    }

    private CartResponse buildCartResponse(
            UUID userId,
            List<CartItem> items,
            boolean itemsPurged,
            boolean itemsAdjusted
    ) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalUnits = 0;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            Variant v = item.getVariant();
            BigDecimal price = v.getVariantPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

            totalUnits += item.getQuantity();
            grandTotal = grandTotal.add(subtotal);

            itemResponses.add(new CartItemResponse(
                    v.getVariantId(),
                    v.getProduct().getProductName(),
                    v.getVariantSku(),
                    v.getVariantDesc(),
                    null, //v.getPrimaryImageUrl(), este valor no viene con el registro de variante, hay que buscarlo en la tabla Image
                    price,
                    item.getQuantity(),
                    subtotal,
                    v.getVariantStock(),
                    item.getQuantity() > v.getVariantStock()
            ));
        }

        return new CartResponse(
                userId,
                itemResponses,
                totalUnits,
                grandTotal,
                itemsPurged,
                itemsAdjusted
        );
    }
}