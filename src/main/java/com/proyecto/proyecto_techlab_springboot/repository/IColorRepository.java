package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.Color;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IColorRepository extends JpaRepository<Color, Integer> {
    boolean existsByColorNameIgnoreCase(String colorName);
    boolean existsByColorCodeIgnoreCase(String colorCode);

    // Para el update: existe con ese nombre PERO con un ID distinto al que estoy editando
    boolean existsByColorNameIgnoreCaseAndColorIdNot(String colorName, Integer colorId);
    boolean existsByColorCodeIgnoreCaseAndColorIdNot(String colorCode, Integer colorId);

    List<Color> findAllByRowStatus(RowStatus rowStatus);
}