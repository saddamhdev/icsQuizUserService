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
    @Autowired
    private  ReactiveRedisTemplate<String, User> reactiveRedisTemplate;

    public Mono<User> authenticate(Long userId, String password) {
        String redisKey = "user:" + userId;

        return reactiveRedisTemplate
                .opsForValue()
                .get(redisKey)
                .flatMap(cachedUser -> {
                    if (cachedUser != null && cachedUser.getPassword().equals(password)) {
                        return Mono.just(cachedUser); // FAST PATH 🚀
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(
                        stagingRepo.findByUserId(userId)
                                .next()
                                .flatMap(stagingUser -> {
                                    if (password.equals(stagingUser.getPasswordPlain())) {

                                        // cache into redis for next time
                                        User user = new User(
                                                stagingUser.getUserId(),
                                                stagingUser.getName(),
                                                stagingUser.getCodeNumber(),
                                                stagingUser.getPasswordPlain()
                                        );

                                        return reactiveRedisTemplate.opsForValue()
                                                .set(redisKey, user)
                                                .thenReturn(user);
                                    }
                                    return Mono.empty();
                                })
                )
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials")));
    }

}
