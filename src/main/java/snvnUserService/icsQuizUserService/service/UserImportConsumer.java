package snvnUserService.icsQuizUserService.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import snvnUserService.icsQuizUserService.model.StagingUser;
import snvnUserService.icsQuizUserService.model.User;
import snvnUserService.icsQuizUserService.repository.StagingUserRepository;
import snvnUserService.icsQuizUserService.repository.UserRepository;

import java.time.Duration;

@Service
public class UserImportConsumer {
    @PostConstruct
    public void init() {
        System.out.println("🔄 [KAFKA CONSUMER] UserImportConsumer initialized — listening on topic: user-import");
    }
    @Autowired
    private StagingUserRepository stagingRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private ReactiveRedisTemplate<String, User> redisTemplate;

    @KafkaListener(topics = "user-import-saddam", groupId = "user-import-group")
    public void processUserImport(String message) {
        System.out.println("📥 [KAFKA CONSUMER] Received Kafka event: " + message);
        processBatch()
                .doOnSubscribe(s -> System.out.println("⚙️ [BATCH] Starting to process staging users"))
                .doOnTerminate(() -> System.out.println("✅ [BATCH] Processing complete for " + message))
                .subscribe();
    }


    public Mono<Void> processBatch() {
        long start = System.currentTimeMillis();

        return stagingRepo.findTop500000ByOrderByCreatedAtAsc()
                .flatMap(staging -> {
                    User user = new User();
                    user.setUserId(staging.getUserId());
                    user.setName(staging.getName());
                    user.setCodeNumber(staging.getCodeNumber());
                    user.setPassword(staging.getPasswordPlain()); // 🔸 keep plain password (no encryption)


                    // ✅ Save to Redis and main table, then delete from staging
                    return redisTemplate.opsForValue()
                            .set("user:" + user.getUserId(), user)
                            .then(userRepo.save(user))
                            .flatMap(saved ->
                                    stagingRepo.delete(staging)
                                            .thenReturn(saved)
                            )
                            .doOnNext(saved ->
                                    System.out.println("💾 Saved to main table: " + saved.getUserId() + " | " + saved.getName())
                            )
                            .onErrorResume(e -> {
                                System.err.println("❌ Error processing userId=" + user.getUserId() +
                                        ": " + e.getMessage());
                                return retrySaveUser(staging); // 🔁 custom retry
                            });
                }, 100) // process up to 100 users concurrently
                .then()
                .doOnTerminate(() -> {
                    long elapsed = System.currentTimeMillis() - start;
                    System.out.println("✅ Processed batch in " + elapsed / 1000.0 + " seconds");
                });
    }

    private Mono<User> retrySaveUser(StagingUser staging) {
        return Mono.defer(() -> saveUser(staging))
                .retryWhen(reactor.util.retry.Retry.fixedDelay(5, Duration.ofSeconds(1)))
                .onErrorResume(e -> {
                    System.err.println("🚫 Retry failed for userId=" + staging.getUserId());
                    return Mono.empty();
                });
    }
    private Mono<User> saveUser(StagingUser staging) {
        User user = new User();
        user.setUserId(staging.getUserId());
        user.setName(staging.getName());
        user.setCodeNumber(staging.getCodeNumber());
        user.setPassword(staging.getPasswordPlain());

        return userRepo.save(user)
                .flatMap(saved -> stagingRepo.delete(staging).thenReturn(saved));
    }
}
