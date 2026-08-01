package dev.learn.kafka.orderservice.dispatch.handler;

import dev.learn.kafka.orderservice.dispatch.message.OrderCreated;
import dev.learn.kafka.orderservice.dispatch.service.DispatchService;
import dev.learn.kafka.orderservice.dispatch.util.TestEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class OrderCreatedHandlerTest {
    private OrderCreatedHandler orderCreatedHandler;
    private DispatchService mockDispatchService;

    @BeforeEach
    void setup() {
        mockDispatchService = mock(DispatchService.class);
        orderCreatedHandler = new OrderCreatedHandler(mockDispatchService);
    }

    @Test
    void listen() {
        OrderCreated order = TestEventData.buildOrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "specter",
                1);
        orderCreatedHandler.listen(order);
        verify(mockDispatchService, times(1))
                .process(order);
    }
}