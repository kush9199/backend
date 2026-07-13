package dev.monorepo.shared.authFilter.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@EnableWebSecurity
@EnableConfigurationProperties(JwtAuthFilterProperties.class)
public class JWTSharedSecurityConfig {
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService defaultUserDetailsService(JwtAuthFilterProperties props) {
        // Option B: reflective fallback, driven by properties file
        if (props.getUserDetailsServiceClass() != null) {
            try {
                var className = Class.forName(props.getUserDetailsServiceClass());
                return (UserDetailsService) className.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to instantiate UserDetailsService: " + props.getUserDetailsServiceClass(), e);
            }
        }
        throw new IllegalStateException(
                "No UserDetailsService bean found and shared.auth.user-details-service-class not set");
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder encoder
    ){
        var authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(encoder);
        return new ProviderManager(authProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilterProperties props,
            AuthenticationManager authenticationManager
    ) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> {
            if (!props.getPublicEndpoints().isEmpty()) {
                auth.requestMatchers(props.getPublicEndpoints().toArray(new String[0])).permitAll();
            }
            if (!props.getSecuredEndpoints().isEmpty()) {
                auth.requestMatchers(props.getSecuredEndpoints().toArray(new String[0])).authenticated();
            } else {
                auth.anyRequest().authenticated();
            }
        });
        http.authenticationManager(authenticationManager);
        http.addFilterBefore(
                new JwtSharedCredentialsAuthFilter(props),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
