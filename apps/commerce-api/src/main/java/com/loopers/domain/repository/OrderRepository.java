package com.loopers.domain.repository;

import com.loopers.domain.model.order.Order;
import com.loopers.domain.model.user.UserId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    List<Order> findAllByUserId(UserId userId);
}
