package com.loopers.application.brand;

import com.loopers.domain.model.brand.Brand;
import com.loopers.domain.model.brand.BrandName;
import com.loopers.domain.repository.BrandRepository;
import com.loopers.infrastructure.common.SpringDomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BrandService implements CreateBrandUseCase, UpdateBrandUseCase, DeleteBrandUseCase, BrandQueryUseCase {

    private final BrandRepository brandRepository;
    private final SpringDomainEventPublisher eventPublisher;

    public BrandService(BrandRepository brandRepository, SpringDomainEventPublisher eventPublisher) {
        this.brandRepository = brandRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void createBrand(String name, String description) {
        BrandName brandName = BrandName.of(name);
        if (brandRepository.existsByName(brandName)) {
            throw new IllegalArgumentException("이미 존재하는 브랜드 이름입니다.");
        }
        Brand brand = Brand.create(brandName, description);
        brandRepository.save(brand);
    }

    @Override
    @Transactional
    public void updateBrand(Long brandId, String name, String description) {
        Brand brand = findBrand(brandId);
        Brand updated = brand.update(BrandName.of(name), description);
        brandRepository.save(updated);
    }

    @Override
    @Transactional
    public void deleteBrand(Long brandId) {
        Brand brand = findBrand(brandId);
        Brand deleted = brand.delete();
        brandRepository.save(deleted);
        eventPublisher.publishEvents(deleted);
    }

    @Override
    public BrandInfo getBrand(Long brandId) {
        Brand brand = findBrand(brandId);
        return toBrandInfo(brand);
    }

    @Override
    public List<BrandInfo> getBrands() {
        return brandRepository.findAll().stream()
                .filter(brand -> !brand.isDeleted())
                .map(this::toBrandInfo)
                .toList();
    }

    private Brand findBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .filter(brand -> !brand.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));
    }

    private BrandInfo toBrandInfo(Brand brand) {
        return new BrandInfo(brand.getId(), brand.getName().getValue(), brand.getDescription());
    }
}
