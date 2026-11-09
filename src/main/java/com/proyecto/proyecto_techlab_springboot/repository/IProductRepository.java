package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IProductRepository extends JpaRepository<Product, UUID> {
    // Para Administradores: Trae ACTIVE e INACTIVE
    @Query("""
        SELECT p FROM Product p
        ORDER BY p.createdAt DESC
    """)
    List<Product> findAllForAdmin();

    // Para los que quieren comprar, solo les muestra productos activos
    @Query("""
        SELECT p FROM Product p
        WHERE p.rowStatus = 'ACTIVE'
        ORDER BY p.createdAt DESC
    """)
    List<Product> findAllPublic();
}