package com.loopers.application.product;

import com.loopers.domain.model.brand.Brand;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.repository.BrandRepository;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductQueryService implements ProductQueryUseCase {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public ProductQueryService(ProductRepository productRepository, BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
    }

    @Override
    public ProductDetailInfo getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));

        return new ProductDetailInfo(
                product.getId(),
                brand.getId(),
                brand.getName().getValue(),
                product.getName().getValue(),
                product.getPrice().getValue(),
                product.getStock().getValue(),
                product.getLikeCount().getValue(),
                product.getDescription().getValueOrNull()
        );
    }

    @Override
    public Page<ProductSummaryInfo> getProducts(Long brandId, String sort, int page, int size) {
        Sort sorting = resolveSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, sorting);

        Page<Product> products = productRepository.findAllByDeletedAtIsNull(brandId, pageRequest);

        return products.map(product -> {
            String brandName = brandRepository.findById(product.getBrandId())
                    .map(b -> b.getName().getValue())
                    .orElse("");
            return new ProductSummaryInfo(
                    product.getId(),
                    product.getBrandId(),
                    brandName,
                    product.getName().getValue(),
                    product.getPrice().getValue(),
                    product.getLikeCount().getValue()
            );
        });
    }

    private Sort resolveSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "likes_desc" -> Sort.by(Sort.Direction.DESC, "likeCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
