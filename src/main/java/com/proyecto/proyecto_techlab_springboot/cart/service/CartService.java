package com.proyecto.proyecto_techlab_springboot.cart.service;

import com.proyecto.proyecto_techlab_springboot.cart.dto.CartItemRequest;
import com.proyecto.proyecto_techlab_springboot.cart.dto.CartItemResponse;
import com.proyecto.proyecto_techlab_springboot.cart.dto.CartItemUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.cart.dto.CartResponse;
import com.proyecto.proyecto_techlab_springboot.cart.mapper.CartMapper;
import com.proyecto.proyecto_techlab_springboot.cart.exception.CartItemNotFoundException;
import com.proyecto.proyecto_techlab_springboot.variant.exception.VariantNoLongerAvailableException;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.common.exception.InsufficientStockException;
import com.proyecto.proyecto_techlab_springboot.cart.model.CartItem;
import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.user.service.UserService;
import com.proyecto.proyecto_techlab_springboot.variant.model.Variant;
import com.proyecto.proyecto_techlab_springboot.cart.model.CartItemId;
import com.proyecto.proyecto_techlab_springboot.cart.repository.ICartItemRepository;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImageKey;
import com.proyecto.proyecto_techlab_springboot.image.service.ImageService;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final ICartItemRepository iCartItemRepository;
    private final CartMapper cartMapper;

    private final UserService userService;
    private final VariantService variantService;
    private final ImageService imageService;

    @Transactional
    public CartResponse getCartByUser(UUID userId) {
        this.userService.findUserByIdOrThrow(userId);
        return fetchAndSyncCartResponse(userId);
    }

    @Transactional(readOnly = true)
    public CartResponse syncAnonymousCart(List<CartItemRequest> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty())
            return this.cartMapper.toResponse(null, List.of(), 0, BigDecimal.ZERO, false, false);

        Map<Long, Long> requestedQuantities = new LinkedHashMap<>();
        for (CartItemRequest item : requestedItems)
            requestedQuantities.merge(item.variantId(), item.quantity().longValue(), Long::sum);

        List<Variant> variants = this.variantService.findAllByIdInWithDetails(
                requestedQuantities.keySet()
        );
        Map<Long, Variant> variantsById = new HashMap<>();
        for (Variant variant : variants)
            variantsById.put(variant.getVariantId(), variant);

        List<CartLine> itemsToKeep = new ArrayList<>();
        boolean itemsPurged = false;
        boolean itemsAdjusted = false;

        for (Map.Entry<Long, Long> requested : requestedQuantities.entrySet()) {
            Variant variant = variantsById.get(requested.getKey());
            Optional<CartLine> reconciledLine = reconcileCartLine(variant, requested.getValue());
            if (reconciledLine.isEmpty()) {
                itemsPurged = true;
                continue;
            }

            CartLine line = reconciledLine.get();
            itemsToKeep.add(line);
            itemsAdjusted |= line.wasAdjusted();
        }

        return buildCartResponse(null, itemsToKeep, itemsPurged, itemsAdjusted);
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
            throw new CartItemNotFoundException(
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

        List<CartLine> itemsToKeep      = new ArrayList<>();
        List<CartItem> itemsToDelete    = new ArrayList<>();

        boolean itemsAdjusted = false;

        for (CartItem item : storedItems) {
            Variant variant = item.getVariant();
            Optional<CartLine> reconciledLine = reconcileCartLine(variant, item.getQuantity());
            if (reconciledLine.isEmpty()) {
                itemsToDelete.add(item);
                continue;
            }

            CartLine line = reconciledLine.get();
            if (line.wasAdjusted()) {
                item.setQuantity(line.quantity());
                itemsAdjusted = true;
            }

            itemsToKeep.add(line);
        }

        if (!itemsToDelete.isEmpty()) {
            // Borrado físico directamente en la DB con una única consulta SQL masiva
            this.iCartItemRepository.deleteAllInBatch(itemsToDelete);
            log.info("Se purgaron {} ítems obsoletos del carrito del usuario {}", itemsToDelete.size(), userId);
        }

        return buildCartResponse(userId, itemsToKeep, !itemsToDelete.isEmpty(), itemsAdjusted);
    }

    private Optional<CartLine> reconcileCartLine(Variant variant, long requestedQuantity) {
        if (
                variant == null
                || variant.getRowStatus() != RowStatus.ACTIVE
                || variant.getProduct().getRowStatus() != RowStatus.ACTIVE
                || variant.getColor().getRowStatus() != RowStatus.ACTIVE
                || variant.getVariantStock() <= 0
        )
            return Optional.empty();

        boolean wasAdjusted = requestedQuantity > variant.getVariantStock();
        int finalQuantity = wasAdjusted
                ? variant.getVariantStock()
                : (int) requestedQuantity;

        return Optional.of(new CartLine(variant, finalQuantity, wasAdjusted));
    }

    private void validateQuantity(int required, int stock) {
        if (required > stock)
            throw new InsufficientStockException("{ Disponible: %d, Solicitado: %d }".formatted(stock, required));
    }

    private CartResponse buildCartResponse(
            UUID            userId,
            List<CartLine>  items,
            boolean         itemsPurged,
            boolean         itemsAdjusted
    ) {
        int                     totalUnits      = 0;
        BigDecimal              grandTotal      = BigDecimal.ZERO;
        List<CartItemResponse>  itemResponses   = new ArrayList<>();
        Map<ImageKey, String> primaryImages = this.imageService.getPrimaryImageUrls(
                items.stream()
                        .map(CartLine::variant)
                        .map(variant -> new ImageKey(
                                variant.getProduct().getProductId(),
                                variant.getColor().getColorId()
                        ))
                        .toList()
        );

        for (CartLine item : items) {
            Variant     v           = item.variant();
            int         quantity    = item.quantity();
            BigDecimal  price       = v.getVariantPrice();
            BigDecimal  subtotal    = price.multiply(BigDecimal.valueOf(quantity));

            totalUnits += quantity;
            grandTotal = grandTotal.add(subtotal);

            itemResponses.add(this.cartMapper.toItemResponse(
                    v,
                    primaryImages.get(new ImageKey(v.getProduct().getProductId(), v.getColor().getColorId())),
                    price,
                    quantity,
                    subtotal,
                    item.wasAdjusted()
            ));
        }

        return this.cartMapper.toResponse(
                userId,
                itemResponses,
                totalUnits,
                grandTotal,
                itemsPurged,
                itemsAdjusted
        );
    }

    private record CartLine(Variant variant, int quantity, boolean wasAdjusted) {}
}