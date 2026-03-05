package com.loopers.interfaces.api.order.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryAddressUpdateRequest(
        @NotBlank(message = "배송 주소는 필수입니다.")
        String address
) {}
