package snvnUserService.icsQuizUserService.service;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "webflux_topic";

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<String> sendMessage(String message) {
        // send() now returns CompletableFuture
        return Mono.fromFuture(
                kafkaTemplate.send(TOPIC, message)
                        .thenApply(result ->
                                "✅ Sent message to partition " + result.getRecordMetadata().partition()
                        )
                        .exceptionally(ex ->
                                "❌ Error sending message: " + ex.getMessage()
                        )
        );
    }
}
