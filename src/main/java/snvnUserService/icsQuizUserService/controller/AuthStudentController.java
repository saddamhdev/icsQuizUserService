package snvnUserService.icsQuizUserService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import snvnUserService.icsQuizUserService.dto.LoginRequest;
import snvnUserService.icsQuizUserService.service.AuthService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student/auth")
public class AuthStudentController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        long startTime = System.currentTimeMillis(); // ⏱️ Start timer
        System.out.println("🚀 Login attempt for userId=" + request.getUserId());

        return authService.authenticate(request.getUserId(), request.getPassword())
                .map(user -> {
                    long elapsed = System.currentTimeMillis() - startTime; // ⏱️ Stop timer
                    double seconds = elapsed / 1000.0;

                    System.out.printf("✅ Login successful for userId=%d in %.3f seconds (%d ms)%n",
                            user.getUserId(), seconds, elapsed);

                    Map<String, Object> body = Map.of(
                            "message", "✅ Login successful",
                            "userId", user.getUserId(),
                            "name", user.getName(),
                            "timeTakenMs", elapsed,
                            "timeTakenSec", seconds
                    );
                    return ResponseEntity.ok(body);
                })
                .onErrorResume(e -> {
                    long elapsed = System.currentTimeMillis() - startTime; // ⏱️ Stop timer (even for errors)
                    double seconds = elapsed / 1000.0;

                    System.out.printf("❌ Login failed for userId=%d in %.3f seconds (%d ms): %s%n",
                            request.getUserId(), seconds, elapsed, e.getMessage());

                    Map<String, Object> errorBody = Map.of(
                            "error", e.getMessage(),
                            "timeTakenMs", elapsed,
                            "timeTakenSec", seconds
                    );
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
                });
    }



}

