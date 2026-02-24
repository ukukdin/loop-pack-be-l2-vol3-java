package com.loopers.application.service;

import com.loopers.application.CreateProductUseCase;
import com.loopers.application.DeleteProductUseCase;
import com.loopers.application.UpdateProductUseCase;
import com.loopers.domain.model.product.Price;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.product.ProductName;
import com.loopers.domain.model.product.Stock;
import com.loopers.domain.repository.BrandRepository;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService implements CreateProductUseCase, UpdateProductUseCase, DeleteProductUseCase {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public ProductService(ProductRepository productRepository, BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
    }

    @Override
    public void createProduct(Long brandId, String name, int price, int stock, String description) {
        brandRepository.findById(brandId)
                .filter(brand -> !brand.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 브랜드입니다."));

        Product product = Product.create(brandId, ProductName.of(name), Price.of(price), Stock.of(stock), description);
        productRepository.save(product);
    }

    @Override
    public void updateProduct(Long productId, String name, int price, int stock, String description) {
        Product product = findProduct(productId);
        Product updated = product.update(ProductName.of(name), Price.of(price), Stock.of(stock), description);
        productRepository.save(updated);
    }

    @Override
    public void deleteProduct(Long productId) {
        Product product = findProduct(productId);
        Product deleted = product.delete();
        productRepository.save(deleted);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(product -> !product.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }
}
