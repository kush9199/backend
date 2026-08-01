package dev.learn.kafka.orderservice.dispatch.util;

import dev.learn.kafka.orderservice.dispatch.dto.Item;
import dev.learn.kafka.orderservice.dispatch.message.OrderCreated;

import java.util.UUID;

public class TestEventData {
    public static OrderCreated buildOrderCreatedEvent(UUID orderId, UUID itemId, String itemName, int itemQuantity) {
        return OrderCreated.builder()
                .id(orderId)
                .item(Item.builder()
                        .id(itemId)
                        .name(itemName)
                        .quantity(itemQuantity).build()).build();
    }
}
