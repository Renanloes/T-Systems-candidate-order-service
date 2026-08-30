package com.tsystems.challenge.orders;

import com.tsystems.challenge.orders.domain.Order;
import com.tsystems.challenge.orders.domain.OrderStatus;
import com.tsystems.challenge.orders.dto.CreateOrderRequest;
import com.tsystems.challenge.orders.repository.InMemoryOrderRepository;
import com.tsystems.challenge.orders.service.OrderService;
import com.tsystems.challenge.orders.service.PricingApiClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Test
    void createsAndConfirmsAnOrderUsingThePricingApi() {
        PricingApiClient pricingApiClient = mock(PricingApiClient.class);

        when(pricingApiClient.getPrice(any(CreateOrderRequest.class)))
                .thenReturn(new BigDecimal("19.99"));

        OrderService service = new OrderService(
                new InMemoryOrderRepository(),
                pricingApiClient
        );

        Order order = service.create(new CreateOrderRequest(
                "customer-42",
                "SKU-1001",
                2,
                "DE",
                "EUR"
        ));

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.unitPrice()).isEqualByComparingTo("19.99");
        assertThat(order.totalPrice()).isEqualByComparingTo("39.98");
    }
}