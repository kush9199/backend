package dev.learn.kafka.orderservice.dispatch.service;

import dev.learn.kafka.orderservice.dispatch.message.OrderCreated;
import dev.learn.kafka.orderservice.dispatch.util.TestEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DispatchServiceTest {

    private DispatchService service;

    @BeforeEach
    void setUp() {
        service = new DispatchService();
    }

    @Test
    void process() {
        OrderCreated order = TestEventData.buildOrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "specter",
                1);
        service.process(order);
    }
}