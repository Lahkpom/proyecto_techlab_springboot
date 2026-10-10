package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.repository.IVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VariantService {

    private final IVariantRepository iVariantRepository;


}
