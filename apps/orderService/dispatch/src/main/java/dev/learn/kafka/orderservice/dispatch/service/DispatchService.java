package dev.learn.kafka.orderservice.dispatch.service;

import dev.learn.kafka.orderservice.dispatch.message.OrderCreated;
import org.springframework.stereotype.Service;

@Service
public class DispatchService {

    public void process(OrderCreated payload) {
        // further processing
    }
}
