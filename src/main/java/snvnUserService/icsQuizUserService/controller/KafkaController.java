package snvnUserService.icsQuizUserService.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import snvnUserService.icsQuizUserService.service.KafkaProducerService;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private final KafkaProducerService kafkaProducerService;

    public KafkaController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/publish")
    public Mono<String> publish(@RequestParam String message) {
        return kafkaProducerService.sendMessage(message);
    }
}
