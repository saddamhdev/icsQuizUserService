package snvnUserService.icsQuizUserService.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import snvnUserService.icsQuizUserService.model.StagingUser;
import snvnUserService.icsQuizUserService.model.User;

@Repository
public interface StagingUserRepository extends ReactiveCrudRepository<StagingUser, Long> {

    // ✅ Find latest uploaded users (for batch migration or processing)
    Flux<StagingUser> findTop500000ByOrderByCreatedAtAsc();

    // ✅ Find all users by codeNumber (if you need deduplication or validation)
    Flux<StagingUser> findByCodeNumber(String codeNumber);

    // ✅ Fetch paged data using LIMIT + OFFSET
    @Query("SELECT * FROM staging_users ORDER BY id ASC LIMIT :limit OFFSET :offset")
    Flux<StagingUser> findBatch(int limit, long offset);
    Flux<StagingUser> findByUserId(Long userId);

}
