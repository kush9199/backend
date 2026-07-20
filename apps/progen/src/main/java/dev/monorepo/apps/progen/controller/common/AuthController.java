package dev.monorepo.apps.progen.controller.common;

import dev.monorepo.apps.progen.dto.request.UserSignupRequest;
import dev.monorepo.apps.progen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        return ResponseEntity.ok("test success");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@NonNull @RequestBody UserSignupRequest userSignupRequest) {
        userRepository
                .findUserByUsername(userSignupRequest.username())
                .ifPresent((user)->{});
    }
}
