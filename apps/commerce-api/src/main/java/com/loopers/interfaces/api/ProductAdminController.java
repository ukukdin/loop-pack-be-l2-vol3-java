package com.loopers.interfaces.api;

import com.loopers.application.CreateProductUseCase;
import com.loopers.application.DeleteProductUseCase;
import com.loopers.application.ProductQueryUseCase;
import com.loopers.application.UpdateProductUseCase;
import com.loopers.interfaces.api.dto.ProductCreateRequest;
import com.loopers.interfaces.api.dto.ProductDetailResponse;
import com.loopers.interfaces.api.dto.ProductUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api-admin/v1/products")
public class ProductAdminController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ProductQueryUseCase productQueryUseCase;

    public ProductAdminController(CreateProductUseCase createProductUseCase,
                                  UpdateProductUseCase updateProductUseCase,
                                  DeleteProductUseCase deleteProductUseCase,
                                  ProductQueryUseCase productQueryUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.productQueryUseCase = productQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> createProduct(@RequestBody ProductCreateRequest request) {
        createProductUseCase.createProduct(
                request.brandId(), request.name(), request.price(), request.stock(), request.description()
        );
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(@PathVariable Long productId,
                                              @RequestBody ProductUpdateRequest request) {
        updateProductUseCase.updateProduct(
                productId, request.name(), request.price(), request.stock(), request.description()
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        deleteProductUseCase.deleteProduct(productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long productId) {
        ProductQueryUseCase.ProductDetailInfo info = productQueryUseCase.getProduct(productId);
        return ResponseEntity.ok(ProductDetailResponse.from(info));
    }
}
