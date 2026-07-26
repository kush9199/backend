package dev.monorepo.shared.authFilter.service;

import dev.monorepo.shared.authFilter.config.JwtAuthFilterProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JWTService {
    private final long JWT_EXPIRATION = 36_00000;
    private final JwtAuthFilterProperties props;
    public JWTService(
            JwtAuthFilterProperties props
    ) {
        this.props = props;
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(props.getJwtSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject, Map<String, ?> claims) {
        return Jwts.builder()
                .signWith(getKey())
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION)).compact();
    }
}
