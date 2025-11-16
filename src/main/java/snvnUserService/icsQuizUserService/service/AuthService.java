package snvnUserService.icsQuizUserService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import snvnUserService.icsQuizUserService.model.User;
import snvnUserService.icsQuizUserService.model.StagingUser;
import snvnUserService.icsQuizUserService.repository.StagingUserRepository;
import snvnUserService.icsQuizUserService.repository.UserRepository;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Autowired
    private  ReactiveRedisTemplate<String, User> redisTemplate;
    @Autowired
    private  StagingUserRepository stagingRepo;
    @Autowired
    private  UserRepository userRepo;

    public Mono<User> authenticate(Long userId, String password) {
        String redisKey = "user:" + userId;

        return redisTemplate.opsForValue()
                .get(redisKey)
                .log("🔍 RedisLookup")
                .flatMap(cachedUser -> {
                    if (cachedUser != null && password.equals(cachedUser.getPassword())) { // ✅ Plain match
                        System.out.println("✅ Found in Redis: " + cachedUser.getName());
                        return Mono.just(cachedUser);
                    }
                    System.out.println("ℹ️ Not found or password mismatch in Redis");
                    return Mono.empty();
                })
                .switchIfEmpty(
                        stagingRepo.findByUserId(userId)
                                .next()
                                .log("🔍 StagingLookup")
                                .flatMap(stagingUser -> {
                                    System.out.println("➡️ Staging user found: " + stagingUser);
                                    if (password.equals(stagingUser.getPasswordPlain())) { // ✅ Plain match
                                        System.out.println("✅ Password matched in Staging Table");
                                        User user = new User();
                                        user.setUserId(stagingUser.getUserId());
                                        user.setName(stagingUser.getName());
                                        user.setCodeNumber(stagingUser.getCodeNumber());
                                        user.setPassword(stagingUser.getPasswordPlain());
                                        return Mono.just(user);
                                    } else {
                                        System.out.println("❌ Password mismatch in Staging Table");
                                        return Mono.empty();
                                    }
                                })
                )
                // Retry logic: Retry for 3 to 5 times on failure
                .retryWhen(Retry.fixedDelay(15, Duration.ofSeconds(3)) // retry 5 times with 2 seconds delay
                        .filter(e -> e instanceof RuntimeException))  // Only retry on specific exception types
                .doOnSubscribe(sub -> System.out.println("🚀 Authenticating userId: " + userId))
                .doOnSuccess(u -> {
                    if (u != null) System.out.println("✅ Authentication Success for " + u.getName());
                })
                .doOnError(err -> System.out.println("💥 Error: " + err.getMessage()))
                .doOnTerminate(() -> System.out.println("🏁 Authentication flow completed"))
                .switchIfEmpty(Mono.error(new RuntimeException("❌ Invalid credentials")));
    }
}
