package dev.monorepo.shared.authFilter.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Component
public class JwtSharedCredentialsAuthFilter extends OncePerRequestFilter {
    private final UserDetailsService userDetailsService;
    private final JwtAuthFilterProperties props;
    public JwtSharedCredentialsAuthFilter(
            JwtAuthFilterProperties props,
            UserDetailsService userDetailsService
    ){
        this.props = props;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var header = request.getHeader("Authorization");

        if(header != null && header.startsWith("Bearer ")){
            var token = header.substring(7);
            try {
                var secretKey = Keys.hmacShaKeyFor(
                        props.getJwtSecretKey().getBytes(StandardCharsets.UTF_8));
                var claims = Jwts.parser()
                        .verifyWith(secretKey).build()
                        .parseSignedClaims(token).getPayload();
                var username = claims.getSubject();
                var user = userDetailsService.loadUserByUsername(username);
                System.out.println("test username: "+user.getUsername());
                if (username.equals(user.getUsername()) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(username, null, user.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ex){
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or Expired JWT Token: " + ex.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
