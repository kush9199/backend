package dev.monorepo.apps.progen.service;

import dev.monorepo.apps.progen.constant.ROLE;
import dev.monorepo.apps.progen.dto.request.AdminSignupRequest;
import dev.monorepo.apps.progen.dto.request.UserSigninRequest;
import dev.monorepo.apps.progen.dto.request.UserSignupRequest;
import dev.monorepo.apps.progen.dto.response.SigninResponse;
import dev.monorepo.apps.progen.model.User;
import dev.monorepo.apps.progen.repository.UserRepository;
import dev.monorepo.shared.responseHandler.common.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;

    private void validateRoleBasedUser(Optional<User> user, ROLE role) {
        try {
            user.ifPresent(updatedUser -> {
                if(updatedUser.isEnable() || !updatedUser.getRole().equals(role)){
                    log.error("singup could only be done via valid credentials: {} is not correct", updatedUser);
                    throw new AppException("INVALID_USER");
                }
                updatedUser.setEnable(true);
                userRepository.save(updatedUser);
            });
        } catch (IllegalArgumentException | OptimisticLockingFailureException ex) {
            log.error("the signup request for user: {} is invalid", user.get());
            throw new AppException("INVALID_USER");
        }
    }

    private void getTraceId() {
        log.info("request processing for: {}", MDC.get("traceId"));
    }

    public void signup(@NonNull UserSignupRequest userSignupRequest) {
        getTraceId();
        userRepository
                .findDistinctUserByUsernameAndEmail(userSignupRequest.username(), userSignupRequest.email())
                .ifPresent((user) -> {
                    log.error("signup could only be done via new user: {} is already present", user);
                    throw new AppException("USER_ALREADY_EXISTS");
                });
        userRepository.save(User.builder()
                .username(userSignupRequest.username())
                .password(encoder.encode(userSignupRequest.password()))
                .email(userSignupRequest.email())
                .role(ROLE.USER)
                .isEnable(true)
                .build());
        log.info("signup for new user successfully : {}", userSignupRequest.username());
    }

    public SigninResponse signin(@NonNull UserSigninRequest userSigninRequest) {
        getTraceId();
        var user = userRepository
                .findUserByUsername(userSigninRequest.username())
                .orElseThrow(() -> {
                    log.error("signin could only be done via existing user: {} is not present", userSigninRequest.username());
                    return new AppException("USER_NOT_FOUND");
                });
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userSigninRequest.username(), userSigninRequest.password())
            );
        } catch (BadCredentialsException | DisabledException | LockedException ex) {
            log.error("the signin request for user: {} is invalid", user);
            throw new AppException("INVALID_USER");
        }
        log.info("signin successfully for user: {}", user);
        return new SigninResponse(user.getUsername(), user.getRole(), user.isEnable());
    }

    public void ownerSignup(@NonNull UserSignupRequest userSignupRequest) {
        getTraceId();
        var user = userRepository.findDistinctUserByUsernameAndEmail(userSignupRequest.username(), userSignupRequest.email());
        if(user.isEmpty()){
            log.error("signup could only be done via existing owner: {}", userSignupRequest.username());
            throw new AppException("INVALID_USER");
        }
        validateRoleBasedUser(user, ROLE.ORG_OWNER);
    }

    public void adminSignup(@NonNull AdminSignupRequest adminSignupRequest) {
        getTraceId();
        var user = userRepository.findDistinctUserByUsernameAndEmail(adminSignupRequest.username(), adminSignupRequest.email());
        if(user.isEmpty()) {
            log.error("signup could only be done via existing admin: {}", adminSignupRequest.username());
            throw new AppException("INVALID_USER");
        }
        validateRoleBasedUser(user, ROLE.ADMIN);
    }
}