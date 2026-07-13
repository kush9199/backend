package dev.monorepo.shared.authFilter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "shared.auth-filter")
public class JwtAuthFilterProperties {
    private String jwtSecretKey;
    private List<String> publicEndpoints = new ArrayList<>();
    private List<String> securedEndpoints = new ArrayList<>();
    private String userDetailsServiceClass;

    public String getUserDetailsServiceClass() {
        return userDetailsServiceClass;
    }

    public void setUserDetailsServiceClass(String userDetailsServiceClass) {
        this.userDetailsServiceClass = userDetailsServiceClass;
    }

    public String getJwtSecretKey() {
        return jwtSecretKey;
    }

    public void setJwtSecretKey(String jwtSecretKey) {
        this.jwtSecretKey = jwtSecretKey;
    }

    public List<String> getPublicEndpoints() {
        return publicEndpoints;
    }

    public void setPublicEndpoints(List<String> publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }

    public List<String> getSecuredEndpoints() {
        return securedEndpoints;
    }

    public void setSecuredEndpoints(List<String> securedEndpoints) {
        this.securedEndpoints = securedEndpoints;
    }
}
