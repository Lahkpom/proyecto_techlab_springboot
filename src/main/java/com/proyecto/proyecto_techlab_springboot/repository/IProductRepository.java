package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IProductRepository extends JpaRepository<Product, UUID> {
    // Para Administradores: Trae ACTIVE e INACTIVE
    @Query("""
        SELECT p FROM Product p
        ORDER BY p.createdAt DESC
    """)
    Page<Product> findAllForAdmin(Pageable pageable);

    // Para los que quieren comprar, solo les muestra productos activos
    @Query("""
        SELECT p FROM Product p
        WHERE p.rowStatus = 'ACTIVE'
        ORDER BY p.createdAt DESC
    """)
    Page<Product> findAllPublic(Pageable pageable);
}