package com.tsystems.challenge.orders.service;

import com.tsystems.challenge.orders.domain.Order;
import com.tsystems.challenge.orders.domain.OrderStatus;
import com.tsystems.challenge.orders.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PricingRetryService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public PricingRetryService(
            OrderRepository orderRepository,
            OrderService orderService
    ) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 10000)
    public void retryPendingOrders() {

        for (Order order : orderRepository.findByStatus(
                OrderStatus.PENDING_PRICING
        )) {
            orderService.tryPriceOrder(order);
        }
    }
}