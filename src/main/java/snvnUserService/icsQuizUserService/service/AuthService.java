package snvnUserService.icsQuizUserService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import snvnUserService.icsQuizUserService.model.User;
import snvnUserService.icsQuizUserService.repository.StagingUserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ReactiveRedisTemplate<String, User> reactiveRedisTemplate;
    private final StagingUserRepository stagingRepo;

    public Mono<User> authenticate(Long userId, String password) {

        String redisKey = "user:" + userId;

        return reactiveRedisTemplate
                .opsForValue()
                .get(redisKey)
                .flatMap(user -> {
                    if (password.equals(user.getPassword())) {
                        return Mono.just(user);    // FAST PATH
                    }
                    return Mono.empty();
                })

                .switchIfEmpty(
                        stagingRepo.findByUserId(userId)
                                .next()
                                .flatMap(staging -> {
                                    if (!password.equals(staging.getPasswordPlain())) {
                                        return Mono.empty();
                                    }

                                    User user = new User(
                                            staging.getUserId(),
                                            staging.getName(),
                                            staging.getCodeNumber(),
                                            staging.getPasswordPlain()
                                    );

                                    return reactiveRedisTemplate.opsForValue()
                                            .set(redisKey, user)
                                            .thenReturn(user);
                                })
                )

                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials")));
    }
}
