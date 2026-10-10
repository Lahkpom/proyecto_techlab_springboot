package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IProductRepository extends JpaRepository<Product, UUID> {
    // Ya incluye save, findAll, findById, delete, etc.
}