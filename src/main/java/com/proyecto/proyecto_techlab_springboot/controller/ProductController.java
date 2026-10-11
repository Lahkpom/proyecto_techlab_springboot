package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.product.ProductCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.product.ProductResponse;
import com.proyecto.proyecto_techlab_springboot.dto.product.ProductUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    //    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest req) {
        ProductResponse res = productService.createProduct(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> res = productService.getAllProducts();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse res = this.productService.getProductById(id);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/{id}")
    //    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable       UUID                    id,
            @Valid @RequestBody ProductUpdateRequest    request
    ) {
        ProductResponse response = this.productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    //    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable       UUID                id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        ProductResponse response = this.productService.updateProductStatus(id, request);
        return ResponseEntity.ok(response);
    }
}