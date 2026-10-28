package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemRequest;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemResponse;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartItemUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.cart.CartResponse;
import com.proyecto.proyecto_techlab_springboot.exception.cart.CartItemNotFoundException;
import com.proyecto.proyecto_techlab_springboot.exception.variant.VariantNoLongerAvailableException;
import com.proyecto.proyecto_techlab_springboot.exception.variant.VariantNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.exception.common.InsufficientStockException;
import com.proyecto.proyecto_techlab_springboot.model.CartItem;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.model.CartItemId;
import com.proyecto.proyecto_techlab_springboot.repository.ICartItemRepository;
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

    private final ICartItemRepository iCartItemRepository;

    private final UserService       userService;
    private final VariantService    variantService;

    @Transactional
    public CartResponse getCartByUser(UUID userId) {
        this.userService.findUserByIdOrThrow(userId);
        return fetchAndSyncCartResponse(userId);
    }

    /**
     * Agrega un item al carrito. Si el item ya existía, se le ajusta la cantidad sumando la nueva y la existente.
     * En ambos casos se chequea previamente que la cantidad no pase el stock disponible de la variante.
     * (Se chequea además que tanto el usuario como la variante, ambos buscados por ID, sean válidos)
     *
     * @param userId Se extrae del token en el CartController
     * @param request CartItemRequest
     * @return CartResponse
     */
    @Transactional
    public CartResponse addItemToCart(UUID userId, CartItemRequest request) {
        Users user = this.userService.findUserByIdOrThrow(userId);

        Variant variant = this.variantService.findVariantByIdOrThrow(request.variantId());

        this.variantService.requireAvailableVariantOrThrow(variant);

        CartItem cartItem = this.iCartItemRepository
                .findById(new CartItemId(userId, variant.getVariantId()))
                .orElse(null);

        int required = request.quantity();

        if (cartItem != null) {
            // Ya existía: sumar la cantidad
            required += cartItem.getQuantity();

            validateQuantity(required, variant.getVariantStock());

            cartItem.setQuantity(required);
        } else {
            // No existía: validar stock inicial e insertar
            validateQuantity(required, variant.getVariantStock());

            cartItem = new CartItem(user, variant, required);

            this.iCartItemRepository.save(cartItem);
        }

        // Devolver el carrito completo y actualizado
        return fetchAndSyncCartResponse(userId);
    }

    @Transactional(noRollbackFor = VariantNoLongerAvailableException.class)
    public CartResponse updateItemQuantity(UUID userId, Long variantId, CartItemUpdateRequest request) {
        this.userService.findUserByIdOrThrow(userId);

        CartItem cartItem = this.iCartItemRepository
                .findById(new CartItemId(userId, variantId))
                .orElseThrow(() -> new CartItemNotFoundException("{ ID: %s }".formatted(variantId)));

        Variant variant = cartItem.getVariant();
        if (variant.getRowStatus() != RowStatus.ACTIVE) {
            this.iCartItemRepository.delete(cartItem);
            this.variantService.requireAvailableVariantOrThrow(variant);
        }

        validateQuantity(request.quantity(), variant.getVariantStock());

        cartItem.setQuantity(request.quantity());

        return fetchAndSyncCartResponse(userId);
    }

    @Transactional
    public void removeItemFromCart(UUID userId, Long variantId) {
        int deleted = this.iCartItemRepository.deleteByUserIdAndVariantId(userId, variantId);

        if (deleted == 0)
            throw new VariantNotFoundException(
                    "No exite un registro para el usuario que realiza la consulta donde en su carrito contenga la variante { ID: %s }"
                            .formatted(variantId)
            );
    }

    @Transactional
    public void clearCart(UUID userId) {
        this.userService.findUserByIdOrThrow(userId);
        this.iCartItemRepository.deleteAllByUserId(userId);
    }

    /**
     * Devuelve el carrito de compras por userId, luego de una filtración de aquellos productos que pudieron ser eliminados o que el stock actual de la variante
     * esté en conflicto con la cantidad que fue guardada.
     *
     * @param userId userId
     * @return CartResponse
     */
    private CartResponse fetchAndSyncCartResponse(UUID userId) {
        List<CartItem> storedItems = this.iCartItemRepository.findAllByUserIdWithDetails(userId);

        List<CartItem> itemsToKeep      = new ArrayList<>();
        List<CartItem> itemsToDelete    = new ArrayList<>();

        boolean itemsAdjusted = false;

        for (CartItem item : storedItems) {
            Variant variant = item.getVariant();

            int currentStock = variant.getVariantStock();

            if (
                    variant.getRowStatus() != RowStatus.ACTIVE
                    || currentStock <= 0
            ) {
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
            // Borrado físico directamente en la DB con una única consulta SQL masiva
            this.iCartItemRepository.deleteAllInBatch(itemsToDelete);
            log.info("Se purgaron {} ítems obsoletos del carrito del usuario {}", itemsToDelete.size(), userId);
        }

        return buildCartResponse(userId, itemsToKeep, !itemsToDelete.isEmpty(), itemsAdjusted);
    }

    private void validateQuantity(int required, int stock) {
        if (required > stock)
            throw new InsufficientStockException("{ Disponible: %d, Solicitado: %d }".formatted(stock, required));
    }

    private CartResponse buildCartResponse(
            UUID            userId,
            List<CartItem>  items,
            boolean         itemsPurged,
            boolean         itemsAdjusted
    ) {
        int                     totalUnits      = 0;
        BigDecimal              grandTotal      = BigDecimal.ZERO;
        List<CartItemResponse>  itemResponses   = new ArrayList<>();

        for (CartItem item : items) {
            Variant     v           = item.getVariant();
            BigDecimal  price       = v.getVariantPrice();
            BigDecimal  subtotal    = price.multiply(BigDecimal.valueOf(item.getQuantity()));

            totalUnits += item.getQuantity();
            grandTotal = grandTotal.add(subtotal);

            itemResponses.add(
                    new CartItemResponse(
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
                    )
            );
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