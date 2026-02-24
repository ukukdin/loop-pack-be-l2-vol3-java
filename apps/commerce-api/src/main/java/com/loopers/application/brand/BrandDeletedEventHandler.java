package com.loopers.application.brand;

import com.loopers.domain.model.brand.event.BrandDeletedEvent;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class BrandDeletedEventHandler {

    private final ProductRepository productRepository;

    public BrandDeletedEventHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(BrandDeletedEvent event) {
        List<Product> products = productRepository.findAllByBrandId(event.brandId());
        for (Product product : products) {
            if (!product.isDeleted()) {
                productRepository.save(product.delete());
            }
        }
    }
}
