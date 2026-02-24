package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductQueryUseCase;
import com.loopers.interfaces.api.product.dto.ProductDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductQueryUseCase productQueryUseCase;

    public ProductController(ProductQueryUseCase productQueryUseCase) {
        this.productQueryUseCase = productQueryUseCase;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long productId) {
        ProductQueryUseCase.ProductDetailInfo info = productQueryUseCase.getProduct(productId);
        return ResponseEntity.ok(ProductDetailResponse.from(info));
    }
}
