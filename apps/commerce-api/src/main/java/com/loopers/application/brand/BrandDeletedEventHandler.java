package com.loopers.application.brand;

import com.loopers.domain.model.brand.event.BrandDeletedEvent;
import com.loopers.domain.model.brand.event.BrandProductsDeletedEvent;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BrandDeletedEventHandler {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BrandDeletedEventHandler(ProductRepository productRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void handle(BrandDeletedEvent event) {
        List<Product> products = productRepository.findAllByBrandId(event.brandId());
        List<Long> deletedProductIds = new ArrayList<>();
        for (Product product : products) {
            if (!product.isDeleted()) {
                productRepository.save(product.delete());
                deletedProductIds.add(product.getId());
            }
        }
        eventPublisher.publishEvent(new BrandProductsDeletedEvent(event.brandId(), deletedProductIds));
    }
}
