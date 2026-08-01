package dev.monorepo.apps.progen.controller.common;

import dev.monorepo.apps.progen.dto.request.AdminSignupRequest;
import dev.monorepo.apps.progen.dto.request.UserSigninRequest;
import dev.monorepo.apps.progen.dto.request.UserSignupRequest;
import dev.monorepo.apps.progen.service.AuthService;
import dev.monorepo.shared.authFilter.service.JWTService;
import dev.monorepo.shared.responseHandler.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JWTService jwtService;
    private final AuthService authService;

    @PostMapping("/admin-signup")
    public ResponseEntity<?> adminSignup(@NonNull @RequestBody AdminSignupRequest adminSignupRequest) {
        authService.adminSignup(adminSignupRequest);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest().replacePath("/auth/signin")
                .build().toUri();
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/owner-signup")
    public ResponseEntity<?> ownerSignup(@NonNull @RequestBody UserSignupRequest userSignupRequest) {
        authService.ownerSignup(userSignupRequest);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest().replacePath("/auth/signin")
                .build().toUri();
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@NonNull @RequestBody UserSignupRequest userSignupRequest) {
        authService.signup(userSignupRequest);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest().replacePath("/auth/signin")
                .build().toUri();
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/signin")
    public ResponseEntity<@NonNull ApiResponse<?>> signin(@NonNull @RequestBody UserSigninRequest userSigninRequest){
        var user = authService.signin(userSigninRequest);
        var token = jwtService
                .generateToken(
                        user.username(),
                        Map.of("roles", user.role().name(),
                                "enabled", user.isEnable()));
        return ResponseEntity.ok(ApiResponse.success(Map.of("token", token)));
    }

}
