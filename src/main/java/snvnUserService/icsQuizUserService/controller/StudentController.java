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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.BaseStream;

@RestController
@RequestMapping("/student/api/users")
public class StudentController {

    @PostConstruct
    public void init() {
        System.out.println("✅ StudentController initialized successfully — repository autowired: " + (userRepository != null));
        System.out.println("🧩 Available CPU cores: " + Runtime.getRuntime().availableProcessors());
    }

    @Autowired
    private final UserCsvGenerator generator;

    public StudentController(UserCsvGenerator generator) {
        this.generator = generator;
    }

    @Autowired private UserImportProducer kafkaProducer;
    @Autowired private StagingUserRepository stagingUserRepository;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ReactiveRedisTemplate<String, User> redisTemplate;

    // 🔸 Keep encoder but not using it
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ✅ Save new user (no encryption)
    @PostMapping("/save")
    public Mono<ResponseEntity<?>> saveUser(@RequestBody User user) {
        return userRepository.existsByCodeNumber(user.getCodeNumber())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(ResponseEntity.badRequest().body("❌ User already exists"));
                    }

                    // 🔸 No encryption applied
                    // user.setPassword(passwordEncoder.encode(user.getPassword()));

                    return userRepository.save(user)
                            .flatMap(saved ->
                                    redisTemplate.opsForValue()
                                            .set("user:" + saved.getUserId(), saved)
                                            .thenReturn(ResponseEntity.ok(saved))
                            );
                });
    }

    // ✅ Generate random users (plain password)
    @PostMapping("/generateRandomUsers")
    public Mono<ResponseEntity<String>> generateRandomUsers(
            @RequestParam int count,
            @RequestParam(defaultValue = "6") int length) {

        long startTime = System.currentTimeMillis();

        return Flux.range(1, count)
                .flatMap(i -> {
                    String code = generateRandomCode(length);
                    User user = new User();
                    user.setUserId(code.hashCode() + System.currentTimeMillis());
                    user.setName("User_" + code);
                    user.setCodeNumber(code);
                    user.setPassword("default123"); // plain password

                    return userRepository.existsByCodeNumber(code)
                            .flatMap(exists -> {
                                if (exists) return Mono.empty();
                                return userRepository.save(user)
                                        .flatMap(saved ->
                                                redisTemplate.opsForValue()
                                                        .set("user:" + saved.getUserId(), saved)
                                                        .thenReturn(saved)
                                        );
                            });
                }, 10)
                .collectList()
                .map(list -> {
                    long endTime = System.currentTimeMillis();
                    String msg = "✅ Inserted " + list.size() + " random users successfully in " +
                            ((endTime - startTime) / 1000.0) + " seconds.";
                    System.out.println(msg);
                    return ResponseEntity.ok(msg);
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError().body("❌ " + ex.getMessage())));
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

    // ✅ Save direct users (plain password)
    @PostMapping("/saveDirect")
    public Mono<ResponseEntity<String>> saveDirectUsers(
            @RequestParam int count,
            @RequestParam(defaultValue = "6") int length) {

        long start = System.currentTimeMillis();

        return Flux.range(1, count)
                .flatMap(i -> {
                    String code = generateRandomCode(length);
                    User user = new User();
                    user.setUserId(code.hashCode() + System.currentTimeMillis());
                    user.setName("DirectUser_" + code);
                    user.setCodeNumber(code);
                    user.setPassword("default123"); // plain password

                    return userRepository.existsByCodeNumber(code)
                            .flatMap(exists -> exists ? Mono.empty() : userRepository.save(user));
                }, 50)
                .collectList()
                .map(list -> {
                    long end = System.currentTimeMillis();
                    String msg = "✅ Inserted " + list.size() + " users directly in " +
                            ((end - start) / 1000.0) + " seconds.";
                    System.out.println(msg);
                    return ResponseEntity.ok(msg);
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError()
                        .body("❌ Error: " + ex.getMessage())));
    }

    // ✅ Save hashed users (but disabled hashing for speed)
    @PostMapping("/saveHashed")
    public Mono<ResponseEntity<String>> saveHashedUsers(
            @RequestParam int count,
            @RequestParam(defaultValue = "6") int length) {

        long start = System.currentTimeMillis();

        return Flux.range(1, count)
                .flatMap(i -> {
                    String code = generateRandomCode(length);
                    String rawPassword = "pass" + code;

                    // 🔸 store plain password
                    // String hashedPassword = passwordEncoder.encode(rawPassword);

                    User user = new User();
                    user.setUserId(code.hashCode() + System.currentTimeMillis());
                    user.setName("HashedUser_" + code);
                    user.setCodeNumber(code);
                    user.setPassword(rawPassword); // store plain

                    return userRepository.existsByCodeNumber(code)
                            .flatMap(exists -> exists ? Mono.empty() : userRepository.save(user));
                }, 20)
                .collectList()
                .map(list -> {
                    long end = System.currentTimeMillis();
                    String msg = "✅ Inserted " + list.size() + " users (no hashing) in " +
                            ((end - start) / 1000.0) + " seconds.";
                    System.out.println(msg);
                    return ResponseEntity.ok(msg);
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError()
                        .body("❌ Error: " + ex.getMessage())));
    }

    // ✅ Upload & insert users from CSV
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Mono<ResponseEntity<String>> uploadAndSaveUsers(@RequestPart("file") FilePart filePart) {
        String uploadDir = "uploads";
        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        File destFile = new File(uploadDir, filePart.filename());

        return filePart.transferTo(destFile)
                .thenMany(readCsvAndSaveUsers(destFile))
                .count()
                .map(count -> ResponseEntity.ok("✅ Uploaded & inserted " + count + " users successfully."))
                .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError().body("❌ " + ex.getMessage())));
    }

    private Flux<User> readCsvAndSaveUsers(File file) {
        return Flux.using(
                () -> Files.lines(file.toPath()),
                stream -> Flux.fromStream(stream)
                        .skip(0)
                        .map(line -> line.split(","))
                        .filter(tokens -> tokens.length >= 5)
                        .map(tokens -> {
                            User user = new User();
                            user.setUserId(tokens[1].trim().isEmpty() ? null : Long.parseLong(tokens[1].trim()));
                            user.setName(tokens[2].trim());
                            user.setCodeNumber(tokens[3].trim());
                            user.setPassword(tokens[4].trim()); // plain
                            user.setCreatedAt(java.time.LocalDateTime.now());
                            user.setUpdatedAt(java.time.LocalDateTime.now());
                            return user;
                        })
                        .as(userRepository::saveAll),
                BaseStream::close
        );
    }

    @PostMapping(value = "/uploadKafka", consumes = "multipart/form-data")
    public Mono<ResponseEntity<String>> uploadCsv(@RequestPart("file") FilePart filePart) {
        String uploadDir = "uploads";
        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        File destFile = new File(directory, filePart.filename());
        long startTime = System.currentTimeMillis();

        System.out.println("📁 [UPLOAD] Starting upload for file: " + filePart.filename());

        return filePart.transferTo(destFile)
                .doOnSuccess(v -> System.out.println("✅ [UPLOAD] File transferred to: " + destFile.getAbsolutePath()))
                .thenMany(parseCsvAndInsertToStaging(destFile.toPath()))
                .doOnNext(user -> System.out.println("📋 [PARSE] Parsed staging user: " + user.getName() + " | Code: " + user.getCodeNumber()))
                .count()
                .flatMap(count -> {
                    double sec = (System.currentTimeMillis() - startTime) / 1000.0;
                    String msg = String.format("✅ Uploaded & inserted %d staging users in %.2f sec — triggering Kafka import.", count, sec);
                    System.out.println(msg);

                    // 🔹 Debug before Kafka send
                    System.out.println("📤 [KAFKA] Sending import event for file: " + filePart.filename());

                    return kafkaProducer.sendImportEvent(filePart.filename())
                            .doOnSuccess(v -> System.out.println("✅ [KAFKA] Event sent successfully for: " + filePart.filename()))
                            .thenReturn(ResponseEntity.ok(msg));
                })
                .onErrorResume(ex -> {
                    ex.printStackTrace();
                    System.err.println("❌ [ERROR] UploadKafka failed: " + ex.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body("❌ " + ex.getMessage()));
                });
    }


    private Flux<StagingUser> parseCsvAndInsertToStaging(Path filePath) {
        int batchSize = 2500;
        int concurrency = 50;

        System.out.println("📖 [PARSE] Starting CSV read from: " + filePath.toAbsolutePath());

        return Flux.using(
                        () -> Files.lines(filePath),
                        stream -> Flux.fromStream(stream)
                                .skip(1)
                                .map(line -> line.split(","))
                                .filter(tokens -> tokens.length >= 5)
                                .map(tokens -> {
                                    StagingUser user = new StagingUser();
                                    try {
                                        user.setUserId(tokens[1].trim().isEmpty() ? null : Long.parseLong(tokens[1].trim()));
                                        user.setName(tokens[2].trim());
                                        user.setCodeNumber(tokens[3].trim());
                                        user.setPasswordPlain(tokens[4].trim());
                                    } catch (Exception e) {
                                        System.err.println("⚠️ [PARSE ERROR] Line: " + String.join(",", tokens));
                                    }
                                    return user;
                                })
                                .buffer(batchSize)
                                .flatMap(batch -> stagingUserRepository.saveAll(batch)
                                                .thenMany(Flux.fromIterable(batch))
                                                .doOnComplete(() -> System.out.println("💾 [DB] Inserted batch of " + batch.size() + " users")),
                                        concurrency),
                        BaseStream::close
                )
                .doOnComplete(() -> System.out.println("✅ [PARSE] Finished reading and inserting CSV"));
    }


    @PostMapping("/generateCsv")
    public String generateCsv(@RequestParam(defaultValue = "10") int count) {
        String filePath = "D:/generated_users.csv";
        generator.generateRandomUsersToCsv(count, filePath);
        return "✅ CSV generated successfully: " + filePath;
    }

    @PostMapping("/readCsvFromUploads")
    public Mono<ResponseEntity<String>> readCsvFromUploads(
            @RequestParam(defaultValue = "generated_users.csv") String filename) {

        String uploadDir = "uploads";
        Path filePath = Paths.get(uploadDir, filename);

        if (!Files.exists(filePath)) {
            return Mono.just(ResponseEntity.badRequest().body("❌ File not found: " + filePath.toAbsolutePath()));
        }

        long startTime = System.currentTimeMillis();

        return parseCsvAndInsertToStaging(filePath)
                .count()
                .flatMap(count -> {
                    double sec = (System.currentTimeMillis() - startTime) / 1000.0;
                    String msg = String.format("✅ Read %s and inserted %d staging users in %.2f sec — triggering Kafka import.",
                            filename, count, sec);
                    System.out.println(msg);
                    return kafkaProducer.sendImportEvent(filename)
                            .thenReturn(ResponseEntity.ok(msg));
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError().body("❌ " + ex.getMessage())));
    }

    @GetMapping
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<User>> getUser(@PathVariable Long id) {
        String key = "user:" + id;
        return redisTemplate.opsForValue().get(key)
                .flatMap(u -> Mono.just(ResponseEntity.ok(u)))
                .switchIfEmpty(
                        userRepository.findById(id)
                                .flatMap(u -> redisTemplate.opsForValue().set(key, u).thenReturn(ResponseEntity.ok(u)))
                                .defaultIfEmpty(ResponseEntity.notFound().build())
                );
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteUser(@PathVariable Long id) {
        String key = "user:" + id;
        return userRepository.findById(id)
                .flatMap(u -> userRepository.delete(u)
                        .then(redisTemplate.opsForValue().delete(key))
                        .then(Mono.just(ResponseEntity.ok("✅ User deleted and cache cleared"))))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @GetMapping("/check/redis")
    public Mono<ResponseEntity<String>> checkRedisConnection() {
        String testKey = "health:user:test";
        User testUser = new User();
        testUser.setUserId(System.currentTimeMillis());
        testUser.setName("HealthCheckUser");
        testUser.setCodeNumber("HC001");
        testUser.setPassword("test123");

        return redisTemplate.opsForValue().set(testKey, testUser)
                .then(redisTemplate.opsForValue().get(testKey))
                .map(fetchedUser -> {
                    if (fetchedUser != null && "HealthCheckUser".equals(fetchedUser.getName())) {
                        return ResponseEntity.ok("✅ Redis connection successful. User read: " + fetchedUser.getName());
                    } else {
                        return ResponseEntity.internalServerError().body("❌ Redis connection failed — unable to read user.");
                    }
                })
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity.internalServerError()
                                .body("❌ Redis error: " + ex.getMessage())));
    }

    // ✅ Check Kafka connection
    @GetMapping("/check/kafka")
    public Mono<ResponseEntity<String>> checkKafkaConnection() {
        String topic = "health-check";
        String message = "ping_" + System.currentTimeMillis();

        try {
            kafkaTemplate.send(topic, message);
            return Mono.just(ResponseEntity.ok("✅ Kafka message sent successfully to topic '" + topic + "'"));
        } catch (Exception ex) {
            return Mono.just(ResponseEntity.internalServerError()
                    .body("❌ Kafka send failed: " + ex.getMessage()));
        }
    }
    // ✅ Fetch all users from user table
    @GetMapping("/all-users")
    public Mono<ResponseEntity<?>> getAllUserData() {
        return userRepository.findAll()
                .collectList()
                .map(users -> {
                    if (users.isEmpty()) {
                        return ResponseEntity.ok("⚠️ No users found in the user table.");
                    }
                    return ResponseEntity.ok(users);
                })
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity.internalServerError()
                                .body("❌ Error fetching users: " + ex.getMessage())));
    }

    // ✅ Fetch all staging users from staging_user table
    @GetMapping("/all-staging-users")
    public Mono<ResponseEntity<?>> getAllStagingUserData() {
        return stagingUserRepository.findAll()
                .collectList()
                .map(stagingUsers -> {
                    if (stagingUsers.isEmpty()) {
                        return ResponseEntity.ok("⚠️ No users found in the staging_user table.");
                    }
                    return ResponseEntity.ok(stagingUsers);
                })
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity.internalServerError()
                                .body("❌ Error fetching staging users: " + ex.getMessage())));
    }


}
