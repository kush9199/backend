package dev.learn.kafka.orderservice.dispatch.handler;

import dev.learn.kafka.orderservice.dispatch.message.OrderCreated;
import dev.learn.kafka.orderservice.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedHandler {

    private final DispatchService dispatchService;

    @KafkaListener(
            id="orderConsumerClient",                   // id for this current service
            topics="order.created",                     // topics that this consumer should consume
            groupId = "dispatch.order.created.consumer" // consumer group belonging to the consumers
    )
    public void listen(OrderCreated payload) {
        log.info("Received message: payload: {}", payload);
        dispatchService.process(payload);
    }

}
