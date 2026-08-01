package dev.monorepo.apps.progen.config;

import dev.monorepo.apps.progen.constant.ROLE;
import dev.monorepo.apps.progen.model.User;
import dev.monorepo.apps.progen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UsersConfiguration {
    private final UserRepository userRepository;
    private final List<User> admins = List.of(
            User.builder()
                    .email("admin@progen.com")
                    .username("admin")
                    .isEnable(false)
                    .role(ROLE.ADMIN)
                    .build()
    );

    @EventListener(ApplicationReadyEvent.class)
    public void setUpAdmin(){
        for(var user : admins) {
            var admin = userRepository
                    .findDistinctUserByUsernameAndEmail(user.getUsername(), user.getEmail());
            if (admin.isPresent()) {
                log.info("admin is already setup : {}", user.getUsername());
                continue;
            }
            userRepository.save(user);
            log.info("unverified admin is setup : {}", user.getUsername());
        }
    }
}
