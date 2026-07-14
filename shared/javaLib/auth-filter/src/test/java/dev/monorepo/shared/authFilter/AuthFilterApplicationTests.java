package dev.monorepo.shared.authFilter;

import dev.monorepo.shared.authFilter.config.JWTSharedSecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(
        properties = {
                "shared.auth-filter.jwt-secret-key=Thisshouldbethekeythatistobeinjectedfromapplicationandshouldhaveminimum32bytesbutmax512bytes",
                "shared.auth-filter.public-endpoints[0]=/api/public",
                "shared.auth-filter.secured-endpoints[0]=/api/private"
        },
        classes = {
                JWTSharedSecurityConfig.class,
                AuthFilterApplicationTests.class,
                AuthFilterApplicationTests.TestAppConfig.class
        }
)
@AutoConfigureMockMvc
class AuthFilterApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService mockUserDetailsService;

    @BeforeEach
    void setUp() {
        Mockito.when(mockUserDetailsService.loadUserByUsername("testuser"))
                .thenReturn(new User("testuser", "password", Collections.emptyList()));

        Mockito.when(mockUserDetailsService.loadUserByUsername(Mockito.argThat(arg -> !arg.equals("testuser"))))
                .thenThrow(new RuntimeException("User not found"));
    }


    private static final String TEST_SECRET = "Thisshouldbethekeythatistobeinjectedfromapplicationandshouldhaveminimum32bytesbutmax512bytes";

    private String generateTestToken(String username, long expirationMillis) {
        SecretKey secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }


    @Test
    @Order(1)
    public void testPublicEndpoint_ShouldAllowAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/public"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    public void testSecuredEndpoint_ShouldBlockAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/private"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    public void testSecuredEndpoint_ShouldBlockAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/private")
                        .header("Authorization", "Bearer Invalid.Token.Here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    public void testSecuredEndpoint_ShouldAllowAccessWithValidToken() throws Exception {
        String validToken = generateTestToken("testuser", 3600000);

        mockMvc.perform(get("/api/private")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());

    }

    @Test
    @Order(5)
    public void testSecuredEndpoint_ShouldBlockAccessWithExpiredToken() throws Exception {
        String expiredToken = generateTestToken("testuser", -1000);

        mockMvc.perform(get("/api/private")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }


    @Configuration
    @EnableAutoConfiguration
    @EnableWebMvc
    static class TestAppConfig {

        @RestController
        @RequestMapping("/api")
        static class DummyController {
            @GetMapping("/public")
            public String publicEndpoint() {
                return "Public Data";
            }

            @GetMapping("/private")
            public String privateEndpoint() {
                return "Private Data";
            }
        }
    }

}
