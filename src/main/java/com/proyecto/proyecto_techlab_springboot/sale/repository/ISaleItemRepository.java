package com.proyecto.proyecto_techlab_springboot.sale.repository;

import com.proyecto.proyecto_techlab_springboot.sale.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ISaleItemRepository extends JpaRepository<SaleItem, Long> {

    // Obtener los renglones congelados de una venta
    List<SaleItem> findAllBySale_SaleId(UUID saleId);
}