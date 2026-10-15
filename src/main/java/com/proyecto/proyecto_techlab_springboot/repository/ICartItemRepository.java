package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.CartItem;
import com.proyecto.proyecto_techlab_springboot.model.CartItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICartItemRepository extends JpaRepository<CartItem, CartItemId> {

    @Query("""
        SELECT ci
        FROM CartItem ci
            JOIN FETCH ci.variant v
            JOIN FETCH v.product p
        WHERE ci.id.userId = :userId
    """)
    List<CartItem> findAllByUserIdWithDetails(@Param("userId") UUID userId);

    @Query("""
        SELECT ci
        FROM CartItem ci
        WHERE ci.id.userId = :userId
            AND ci.id.variantId = :variantId
    """)
    Optional<CartItem> findByUserIdAndVariantId(
            @Param("userId") UUID userId,
            @Param("variantId") Long variantId
    );

    @Modifying
    @Query("""
        DELETE FROM CartItem ci
        WHERE ci.id.userId = :userId
    """)
    int deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("""
        DELETE FROM CartItem ci
        WHERE ci.id.userId = :userId
            AND ci.id.variantId = :variantId
    """)
    int deleteByUserIdAndVariantId(
            @Param("userId") UUID userId,
            @Param("variantId") Long variantId
    );
}