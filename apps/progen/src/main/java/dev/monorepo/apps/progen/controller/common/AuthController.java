package dev.monorepo.apps.progen.controller.common;

import dev.monorepo.apps.progen.constant.ROLE;
import dev.monorepo.apps.progen.dto.request.UserSigninRequest;
import dev.monorepo.apps.progen.dto.request.UserSignupRequest;
import dev.monorepo.apps.progen.model.User;
import dev.monorepo.apps.progen.repository.UserRepository;
import dev.monorepo.shared.authFilter.service.JWTService;
import dev.monorepo.shared.responseHandler.common.ApiResponse;
import dev.monorepo.shared.responseHandler.common.AppException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JWTService jwtService;

    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        return ResponseEntity.ok("test success");
    }

    @PostMapping("/signup")
    public ResponseEntity<@NonNull ApiResponse<?>> signup(@NonNull @RequestBody UserSignupRequest userSignupRequest) {
        userRepository
                .findDistinctByUsernameAndEmail(userSignupRequest.username(), userSignupRequest.email())
                .ifPresent((user) -> {
                    throw new AppException("USER_ALREADY_EXISTS");
                });
        userRepository.save(User.builder()
                .username(userSignupRequest.username())
                .password(encoder.encode(userSignupRequest.password()))
                .email(userSignupRequest.email())
                .role(ROLE.USER)
                .build());
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest().replacePath("/signin")
                .build().toUri();
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/signin")
    public ResponseEntity<@NonNull ApiResponse<?>> signin(@NonNull @RequestBody UserSigninRequest userSigninRequest){
        var user = userRepository
                .findUserByUsername(userSigninRequest.username())
                .orElseThrow(() -> new AppException("USER_NOT_FOUND"));
        try{
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userSigninRequest.username(), userSigninRequest.password())
            );
        }catch(BadCredentialsException | DisabledException | LockedException ex){
            ex.printStackTrace();
            throw new AppException("INVALID_USER");
        }
        var token = jwtService.generateToken(user.getUsername(), Map.of("roles", user.getRole().name()));
        return ResponseEntity.ok(ApiResponse.success(Map.of("token", token)));
    }
}
