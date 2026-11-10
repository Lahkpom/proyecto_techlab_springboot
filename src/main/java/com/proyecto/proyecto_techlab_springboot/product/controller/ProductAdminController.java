package com.proyecto.proyecto_techlab_springboot.product.controller;

import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCreateRequest;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
// @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')") // TODO: Descomentar al habilitar Spring Security
public class ProductAdminController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest req) {
        ProductResponse res = this.productService.createProduct(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProductResponse> res = this.productService.getAllAdminProducts(pageable);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse res = this.productService.getAdminProductById(id);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable       UUID                    id,
            @Valid @RequestBody ProductUpdateRequest    request
    ) {
        ProductResponse response = this.productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable       UUID                id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        ProductResponse response = this.productService.updateProductStatus(id, request);
        return ResponseEntity.ok(response);
    }
}