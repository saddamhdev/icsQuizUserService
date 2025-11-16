package snvnUserService.icsQuizUserService.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumerListener {

    @KafkaListener(topics = "webflux_topic", groupId = "webflux-group")
    public void listen(String message) {
        System.out.println("📩 Received message: " + message);
    }
}
