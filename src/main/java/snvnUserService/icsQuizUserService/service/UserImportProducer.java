package snvnUserService.icsQuizUserService.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Service
public class UserImportProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public UserImportProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<Void> sendImportEvent(String batchName) {
        return Mono.fromRunnable(() -> {
            System.out.println("🚀 [KAFKA PRODUCER] Sending message to 'user-import': Start batch " + batchName);
            kafkaTemplate.send("user-import-saddam", "Start batch: " + batchName);
        }).then();
    }
}
