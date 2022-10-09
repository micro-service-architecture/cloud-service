package com.boot.order.service;

import com.boot.order.dto.OrderDto;
import com.boot.order.entity.OrderEntity;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDto);
    OrderDto getOrderByOrderId(String orderId);
    Iterable<OrderEntity> getOrdersByUserId(String userId);
}
