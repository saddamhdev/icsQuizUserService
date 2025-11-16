package snvnUserService.icsQuizUserService.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import snvnUserService.icsQuizUserService.model.User;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<Boolean> existsByCodeNumber(String codeNumber);
    Mono<User> findByCodeNumber(String codeNumber);
    Flux<User> findByUserId(Long userId);

}
