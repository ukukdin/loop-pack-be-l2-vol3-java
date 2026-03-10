package com.loopers.application.brand;

import com.loopers.domain.model.brand.Brand;
import com.loopers.domain.repository.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BrandQueryService implements BrandQueryUseCase {

    private final BrandRepository brandRepository;

    public BrandQueryService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Override
    public BrandInfo getBrand(Long brandId) {
        Brand brand = brandRepository.findActiveById(brandId)
                .orElseThrow(() -> new CoreException(ErrorType.BRAND_NOT_FOUND));
        return toBrandInfo(brand);
    }

    @Override
    public List<BrandInfo> getBrands() {
        return brandRepository.findAllActive().stream()
                .map(this::toBrandInfo)
                .toList();
    }

    private BrandInfo toBrandInfo(Brand brand) {
        return new BrandInfo(brand.getId(), brand.getName().getValue(), brand.getDescription());
    }
}
