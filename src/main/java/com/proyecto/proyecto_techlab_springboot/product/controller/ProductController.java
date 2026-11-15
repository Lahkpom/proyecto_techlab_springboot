package com.proyecto.proyecto_techlab_springboot.product.controller;

import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCatalogResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductResponse;
import com.proyecto.proyecto_techlab_springboot.product.model.TargetGender;
import com.proyecto.proyecto_techlab_springboot.product.service.ProductService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductCatalogResponse>> getProducts(
            @RequestParam(name = "targetGender") TargetGender targetGender,
            @PageableDefault(size = 20) Pageable pageable
        ) {
        Page<ProductCatalogResponse> res = this.productService.getAllPublicProducts(targetGender, pageable);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse res = this.productService.getPublicProductById(id);
        return ResponseEntity.ok(res);
    }
}