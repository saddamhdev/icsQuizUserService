package snvnUserService.icsQuizUserService.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import snvnUserService.icsQuizUserService.model.StagingUser;
import snvnUserService.icsQuizUserService.model.User;
import snvnUserService.icsQuizUserService.repository.StagingUserRepository;
import snvnUserService.icsQuizUserService.repository.UserRepository;
import snvnUserService.icsQuizUserService.service.UserCsvGenerator;
import snvnUserService.icsQuizUserService.service.UserImportProducer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/api/users")
public class UserController {
    @PostConstruct
    public void init() {
        System.out.println("✅ UserController initialized successfully — repository autowired: " + (userRepository != null));
        System.out.println("🧩 Available CPU cores: " + Runtime.getRuntime().availableProcessors());

    }
    @Autowired
    private final UserCsvGenerator generator;
    public UserController(UserCsvGenerator generator) {
        this.generator = generator;
    }
    @Autowired
    private UserImportProducer kafkaProducer;
    @Autowired
    private StagingUserRepository stagingUserRepository;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactiveRedisTemplate<String, User> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ✅ Save new user (and cache in Redis)
    @PostMapping("/save")
    public Mono<ResponseEntity<?>> saveUser(@RequestBody User user) {
        return userRepository.existsByCodeNumber(user.getCodeNumber())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(ResponseEntity.badRequest().body("❌ User already exists"));
                    }
                    // Hash password before saving
                    user.setPassword(passwordEncoder.encode(user.getPassword()));

                    return userRepository.save(user)
                            .flatMap(saved ->
                                    // Cache the user in Redis
                                    redisTemplate.opsForValue()
                                            .set("user:" + saved.getUserId(), saved)
                                            .thenReturn(ResponseEntity.ok(saved))
                            );
                });
    }
    @PostMapping("/generateRandomUsers")
    public Mono<ResponseEntity<String>> generateRandomUsers(
            @RequestParam int count,
            @RequestParam(defaultValue = "6") int length) {

        long startTime = System.currentTimeMillis(); // ⏱️ start tracking

        return Flux.range(1, count)
                .flatMap(i -> {
                    String code = generateRandomCode(length);
                    User user = new User();
                    user.setUserId(code.hashCode() + System.currentTimeMillis());

                    user.setName("User_" + code);
                    user.setCodeNumber(code);
                    user.setPassword(passwordEncoder.encode("default123"));

                    return userRepository.existsByCodeNumber(code)
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.empty(); // skip duplicates
                                }

                                return userRepository.save(user)
                                        .flatMap(saved ->
                                                redisTemplate.opsForValue()
                                                        .set("user:" + saved.getUserId(), saved)
                                                        .thenReturn(saved)
                                        );
                            });
                }, 10) // ⚙️ concurrency hint (10 threads parallel)
                .collectList()
                .map(savedList -> {
                    long endTime = System.currentTimeMillis(); // ⏱️ end tracking
                    long totalMs = endTime - startTime;
                    double totalSeconds = totalMs / 1000.0;

                    String msg = "✅ Inserted " + savedList.size() + " random users successfully in "
                            + totalSeconds + " seconds (" + totalMs + " ms).";
                    System.out.println(msg);
                    return ResponseEntity.ok(msg);
                })
                .onErrorResume(ex -> {
                    long endTime = System.currentTimeMillis();
                    long totalMs = endTime - startTime;
                    System.err.println("❌ Error after " + totalMs + " ms: " + ex.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body("❌ Error: " + ex.getMessage()));
                });
    }


    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @PostMapping("/saveDirect")
    public Mono<ResponseEntity<String>> saveDirectUsers(
            @RequestParam int count,
            @RequestParam(defaultValue = "6") int length) {

        long start = System.currentTimeMillis(); // ⏱️ start time

        return Flux.range(1, count)
                .flatMap(i -> {
                    String code = generateRandomCode(length);
                    User user = new User();
                    user.setUserId(code.hashCode() + System.currentTimeMillis());
                    user.setName("DirectUser_" + code);
                    user.setCodeNumber(code);
                    user.setPassword("default123"); // plain, no hash

                    // Direct DB insert only
                    return userRepository.existsByCodeNumber(code)
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.empty(); // skip duplicates
                                }
                                return userRepository.save(user);
                            });
                }, 50) // run 50 inserts concurrently
                .collectList()
                .map(list -> {
                    long end = System.currentTimeMillis();
                    long totalMs = end - start;
                    double totalSec = totalMs / 1000.0;
                    String msg = "✅ Inserted " + list.size() +
                            " users directly in " + totalSec + " seconds (" + totalMs + " ms)";
                    System.out.println(msg);
                    return ResponseEntity.ok(msg);
                })
                .onErrorResume(ex -> {
                    long end = System.currentTimeMillis();
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("❌ Error after " + (end - start) + " ms: " + ex.getMessage()));
                });
    }

    @PostMapping("/saveHashed")
    public Mono<ResponseEntity<String>> saveHashedUsers(
            @RequestParam int count,
            @RequestParam(defaultValue = "6") int length) {

        long start = System.currentTimeMillis(); // ⏱ Start timer

        return Flux.range(1, count)
                .flatMap(i -> {
                    String code = generateRandomCode(length);
                    String rawPassword = "pass" + code; // 🔑 different password per user

                    // Hash password (CPU-heavy)
                    String hashedPassword = passwordEncoder.encode(rawPassword);

                    User user = new User();
                    user.setUserId(code.hashCode() + System.currentTimeMillis());
                    user.setName("HashedUser_" + code);
                    user.setCodeNumber(code);
                    user.setPassword(hashedPassword);

                    // Skip duplicates and save
                    return userRepository.existsByCodeNumber(code)
                            .flatMap(exists -> {
                                if (exists) return Mono.empty();
                                return userRepository.save(user);
                            });
                }, 20) // limit concurrency for CPU safety
                .collectList()
                .map(list -> {
                    long end = System.currentTimeMillis();
                    long totalMs = end - start;
                    double totalSec = totalMs / 1000.0;
                    String msg = "✅ Inserted " + list.size() +
                            " hashed users (unique passwords) in " + totalSec +
                            " seconds (" + totalMs + " ms)";
                    System.out.println(msg);
                    return ResponseEntity.ok(msg);
                })
                .onErrorResume(ex -> {
                    long end = System.currentTimeMillis();
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("❌ Error after " + (end - start) + " ms: " + ex.getMessage()));
                });
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Mono<ResponseEntity<String>> uploadAndSaveUsers(@RequestPart("file") FilePart filePart) {
        String uploadDir = "uploads";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File destFile = new File(uploadDir, filePart.filename());

        return filePart.transferTo(destFile)
                // ✅ Only start reading AFTER file is fully saved
                .thenMany(readCsvAndSaveUsers(destFile))
                .count()
                .map(count -> ResponseEntity.ok("✅ Uploaded & inserted " + count + " users successfully."))
                .onErrorResume(ex -> {
                    ex.printStackTrace();
                    return Mono.just(ResponseEntity.internalServerError().body("❌ Error: " + ex.getMessage()));
                });
    }


    /**
     * ✅ Helper method: stream CSV lines → map to User → save to DB
     */
    private Flux<User> readCsvAndSaveUsers(File file) {
        return Flux.using(
                () -> Files.lines(file.toPath()),  // safely open stream
                stream -> Flux.fromStream(stream)
                        .skip(0) // ✅ skip header
                        .map(line -> line.split(","))
                        .filter(tokens -> tokens.length >= 4)
                        .map(tokens -> {
                            User user = new User();
                            // CSV: id,user_id,name,code_number,password
                            user.setUserId(tokens[1].trim().isEmpty() ? null : Long.parseLong(tokens[1].trim()));
                            user.setName(tokens[2].trim());
                            user.setCodeNumber(tokens[3].trim());
                            user.setPassword(tokens[4].trim());
                            user.setCreatedAt(java.time.LocalDateTime.now());
                            user.setUpdatedAt(java.time.LocalDateTime.now());
                            return user;
                        })
                        .as(userRepository::saveAll),
                BaseStream::close
        );
    }

    /**
     * ✅ Upload CSV → Save file → Parse → Insert into staging_users
     */
    @PostMapping(value = "/uploadKafka", consumes = "multipart/form-data")
    public Mono<ResponseEntity<String>> uploadCsv(@RequestPart("file") FilePart filePart) {
        String uploadDir = "uploads";
        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        File destFile = new File(directory, filePart.filename());
        long startTime = System.currentTimeMillis();

        return filePart.transferTo(destFile)
                .thenMany(parseCsvAndInsertToStaging(destFile.toPath()))
                .count()
                .flatMap(count -> {
                    long end = System.currentTimeMillis();
                    double sec = (end - startTime) / 1000.0;

                    String msg = String.format(
                            "✅ Uploaded & inserted %d staging users in %.2f sec — triggering Kafka import.",
                            count, sec
                    );
                    System.out.println(msg);

                    // ✅ Send Kafka event to trigger consumer
                    return kafkaProducer.sendImportEvent(filePart.filename())
                            .thenReturn(ResponseEntity.ok(msg));
                })
                .onErrorResume(ex -> {
                    ex.printStackTrace();
                    return Mono.just(ResponseEntity.internalServerError().body("❌ " + ex.getMessage()));
                });
    }


    /**
     * ✅ Helper method — Read CSV & insert to staging_users
     */
    private Flux<StagingUser> parseCsvAndInsertToStaging(Path filePath) {
        int batchSize = 2500;         // ✅ Batch size per save operation
        int concurrency = 70;        // ✅ Number of batches processed in parallel

        return Flux.using(
                () -> Files.lines(filePath),
                stream -> Flux.fromStream(stream)
                        .skip(1) // skip header line
                        .map(line -> line.split(","))
                        .filter(tokens -> tokens.length >= 4)
                        .map(tokens -> {
                            StagingUser user = new StagingUser();
                            user.setUserId(tokens[1].trim().isEmpty() ? null : Long.parseLong(tokens[1].trim()));
                            user.setName(tokens[2].trim());
                            user.setCodeNumber(tokens[3].trim());
                            user.setPasswordPlain(tokens[4].trim());
                            return user;
                        })
                        // ✅ Group lines into batches
                        .buffer(batchSize)
                        // ✅ Process multiple batches concurrently
                        .flatMap(batch -> {
                            long start = System.currentTimeMillis();
                            return stagingUserRepository.saveAll(batch)
                                    .thenMany(Flux.fromIterable(batch))
                                    .doOnComplete(() -> {
                                        long end = System.currentTimeMillis();
                                        double sec = (end - start) / 1000.0;
                                        System.out.printf("✅ Inserted %d users in %.2f sec (batchSize=%d)%n",
                                                batch.size(), sec, batchSize);
                                    });
                        }, concurrency), // ⚙️ Parallelism level
                BaseStream::close
        );
    }




    @PostMapping("/generateCsv")
    public String generateCsv(@RequestParam(defaultValue = "10") int count) {
        String filePath = "D:/generated_users.csv"; // ⚙️ adjust as needed
        generator.generateRandomUsersToCsv(count, filePath);
        return "✅ CSV generated successfully: " + filePath;
    }

    @PostMapping("/readCsvFromUploads")
    public Mono<ResponseEntity<String>> readCsvFromUploads(
            @RequestParam(defaultValue = "generated_users.csv") String filename) {

        String uploadDir = "uploads";
        Path filePath = Paths.get(uploadDir, filename);

        if (!Files.exists(filePath)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body("❌ File not found: " + filePath.toAbsolutePath()));
        }

        long startTime = System.currentTimeMillis();

        return parseCsvAndInsertToStaging(filePath)
                .count()
                .flatMap(count -> {
                    long end = System.currentTimeMillis();
                    double sec = (end - startTime) / 1000.0;
                    String msg = String.format(
                            "✅ Read %s and inserted %d staging users in %.2f sec — triggering Kafka import.",
                            filename, count, sec
                    );
                    System.out.println(msg);

                    // ✅ Trigger Kafka event
                    return kafkaProducer.sendImportEvent(filename)
                            .thenReturn(ResponseEntity.ok(msg));
                })
                .onErrorResume(ex -> {
                    ex.printStackTrace();
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("❌ Error: " + ex.getMessage()));
                });
    }

    // ✅ Get all users
    @GetMapping
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ✅ Get user (from Redis first, else DB)
    @GetMapping("/{id}")
    public Mono<ResponseEntity<User>> getUser(@PathVariable Long id) {
        String key = "user:" + id;
        return redisTemplate.opsForValue().get(key)
                .flatMap(cachedUser -> Mono.just(ResponseEntity.ok(cachedUser)))
                .switchIfEmpty(
                        userRepository.findById(id)
                                .flatMap(user ->
                                        redisTemplate.opsForValue()
                                                .set(key, user)
                                                .thenReturn(ResponseEntity.ok(user))
                                )
                                .defaultIfEmpty(ResponseEntity.notFound().build())
                );
    }

    // ✅ Delete user (and remove from Redis)
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteUser(@PathVariable Long id) {
        String key = "user:" + id;

        return userRepository.findById(id)
                .flatMap(existing ->
                        userRepository.delete(existing)
                                .then(redisTemplate.opsForValue().delete(key))
                                .then(Mono.just(ResponseEntity.ok("✅ User deleted and cache cleared")))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
