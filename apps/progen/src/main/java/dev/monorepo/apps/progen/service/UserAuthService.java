package dev.monorepo.apps.progen.service;

import dev.monorepo.apps.progen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAuthService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        var user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("invalid username"));
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public @NonNull String getPassword() {
                return user.getPassword();
            }

            @Override
            public String getUsername() {
                return user.getUsername();
            }
        };
    }
}
