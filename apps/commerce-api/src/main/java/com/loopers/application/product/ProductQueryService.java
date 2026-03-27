package com.loopers.application.product;

import com.loopers.domain.model.brand.Brand;
import com.loopers.domain.model.common.PageResult;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.repository.BrandRepository;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.infrastructure.cache.CacheConfig;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductQueryService implements ProductQueryUseCase {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public ProductQueryService(ProductRepository productRepository, BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    @Override
    @Cacheable(value = CacheConfig.PRODUCT_DETAIL, key = "#productId")
    public ProductDetailInfo getProduct(Long productId) {
        Product product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new CoreException(ErrorType.BRAND_NOT_FOUND));

        return new ProductDetailInfo(
                product.getId(),
                brand.getId(),
                brand.getName().getValue(),
                product.getName().getValue(),
                product.getPrice().getValue(),
                product.getSalePrice() != null ? product.getSalePrice().getValue() : null,
                product.isOnSale(),
                product.getStock().getValue(),
                product.getLikeCount(),
                product.getDescription()
        );
    }

    @Transactional(readOnly = true)
    @Override
    @Cacheable(value = CacheConfig.PRODUCT_LIST, key = "'brand:' + #brandId + ':sort:' + #sort + ':page:' + #page + ':size:' + #size")
    public PageResult<ProductSummaryInfo> getProducts(Long brandId, String sort, int page, int size) {
        PageResult<Product> products = productRepository.findAllActive(brandId, sort, page, size);

        List<Long> brandIds = products.content().stream()
                .map(Product::getBrandId)
                .distinct()
                .toList();

        Map<Long, String> brandNameMap = brandRepository.findAllByIds(brandIds).stream()
                .collect(Collectors.toMap(Brand::getId, b -> b.getName().getValue()));

        return products.map(product -> new ProductSummaryInfo(
                product.getId(),
                product.getBrandId(),
                brandNameMap.getOrDefault(product.getBrandId(), ""),
                product.getName().getValue(),
                product.getPrice().getValue(),
                product.getSalePrice() != null ? product.getSalePrice().getValue() : null,
                product.isOnSale(),
                product.getLikeCount()
        ));
    }
}
