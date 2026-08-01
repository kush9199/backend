package dev.learn.kafka.orderservice.dispatch.message;

import dev.learn.kafka.orderservice.dispatch.dto.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreated {
    private UUID id;
    private Item item;
}
