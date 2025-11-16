package snvnUserService.icsQuizUserService.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final DatabaseClient databaseClient;

    public DatabaseInitializer(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public void run(String... args) {
        createUsersTable()
                .then(createStagingUsersTable())
                .then()
                .subscribe(
                        null,
                        error -> System.err.println("❌ Table creation failed: " + error.getMessage()),
                        () -> System.out.println("✅ Tables 'users' and 'staging_users' verified or created.")
                );
    }

    // ✅ Main users table (with hashed password)
    private Mono<Void> createUsersTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                user_id BIGINT,
                name VARCHAR(100) NOT NULL,
                code_number VARCHAR(50) NOT NULL,
                password VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT NOW(),
                updated_at TIMESTAMP
            )
        """;
        return databaseClient.sql(sql).then();
    }

    // ✅ Temporary staging table for CSV uploads (plain password, no constraints)
    private Mono<Void> createStagingUsersTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS staging_users (
            id SERIAL PRIMARY KEY,
            user_id BIGINT,
            name VARCHAR(100),
            code_number VARCHAR(50),
            password_plain VARCHAR(255),
            created_at TIMESTAMP DEFAULT NOW(),
            updated_at TIMESTAMP
        )
    """;
        return databaseClient.sql(sql).then();
    }

}
