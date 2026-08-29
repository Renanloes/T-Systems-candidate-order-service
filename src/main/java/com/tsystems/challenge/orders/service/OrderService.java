package com.tsystems.challenge.orders.service;

import com.tsystems.challenge.orders.domain.Order;
import com.tsystems.challenge.orders.domain.OrderStatus;
import com.tsystems.challenge.orders.dto.CreateOrderRequest;
import com.tsystems.challenge.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PricingApiClient pricingApiClient;
    private final Clock clock;

    public OrderService(
            OrderRepository orderRepository,
            PricingApiClient pricingApiClient
    ) {
        this(orderRepository, pricingApiClient, Clock.systemUTC());
    }

    OrderService(
            OrderRepository orderRepository,
            PricingApiClient pricingApiClient,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.pricingApiClient = pricingApiClient;
        this.clock = clock;
    }

    public Order create(CreateOrderRequest request) {

        Order pendingOrder = new Order(
                UUID.randomUUID(),
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.country(),
                request.currency(),
                null,
                null,
                OrderStatus.PENDING_PRICING,
                Instant.now(clock)
        );

        orderRepository.save(pendingOrder);

        return tryPriceOrder(pendingOrder);
    }

    public Order tryPriceOrder(Order order) {

        CreateOrderRequest request = new CreateOrderRequest(
                order.customerId(),
                order.productId(),
                order.quantity(),
                order.country(),
                order.currency()
        );

        try {
            BigDecimal unitPrice = pricingApiClient.getPrice(request);

            BigDecimal totalPrice = unitPrice.multiply(
                    BigDecimal.valueOf(order.quantity())
            );

            Order confirmedOrder = new Order(
                    order.id(),
                    order.customerId(),
                    order.productId(),
                    order.quantity(),
                    order.country(),
                    order.currency(),
                    unitPrice,
                    totalPrice,
                    OrderStatus.CONFIRMED,
                    order.createdAt()
            );

            return orderRepository.save(confirmedOrder);

        } catch (PricingApiException ex) {

            if (ex.requiresAttention()) {
                Order attentionOrder = new Order(
                        order.id(),
                        order.customerId(),
                        order.productId(),
                        order.quantity(),
                        order.country(),
                        order.currency(),
                        null,
                        null,
                        OrderStatus.NEEDS_ATTENTION,
                        order.createdAt()
                );

                return orderRepository.save(attentionOrder);
            }

            return order;
        }
    }

    public Order get(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> list() {
        return orderRepository.findAll();
    }
}