## 1. Overview
___

This specification defines the architecture and implementation for a shared authentication library in Java Spring Boot. The goal is to write the core authentication logic (e.g., token validation, user context extraction) exactly once, while allowing individual consuming applications to define their own protected and public endpoints locally.  
Why we are doing this:  

- **Code Reusability:** Prevents duplicating complex security code (like JWT parsing or external IAM validation) across multiple microservices. 
- **Decentralized Configuration:** Allows each application to maintain strict control over its own routing and security rules.  
- **Total Isolation:** Ensures that one application's security configuration does not bleed into or affect another application's runtime environment.  
    

## 2. Architecture Approach
___

- We will implement this as a Custom Spring Boot Starter.  
- Why a Spring Boot Starter?  
- A starter leverages Spring's auto-configuration capabilities. When a consuming application includes this library as a dependency, Spring Boot will automatically detect it, initialize the shared filter, and bind it to the consuming application's local properties file (`application.yml`). This provides a seamless "plug-and-play" developer experience.  

## 3. Core Components (The "How")
___

**3.1. Configuration Properties Class**  
- Purpose: To map YAML configurations from the consuming application into Java objects.  

**How it works:** 
- We define a `@ConfigurationProperties` bean. Spring will look for properties prefixed with `shared.auth` in the consuming app's `application.yml` and inject them into this class.  

```java
package com.yourcompany.sharedauth.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "shared.auth") 
public class SharedAuthProperties {
    private List < String > publicEndpoints = new ArrayList < > ();
    private List < String > securedEndpoints = new ArrayList < > ();
    public List < String > getPublicEndpoints() {
        return publicEndpoints;
    }
    public void setPublicEndpoints(List < String > publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }
    public List < String > getSecuredEndpoints() {
        return securedEndpoints;
    }
    public void setSecuredEndpoints(List < String > securedEndpoints) {
        this.securedEndpoints = securedEndpoints;
    }
}
```

  
**3.2. Auto-Configuration & Security Filter Chain**  
- Purpose: To dynamically construct the `SecurityFilterChain` based on the injected properties.  

**How it works:**  
- This configuration class reads the `SharedAuthProperties` and applies them to Spring Security's HTTP builder. It registers the endpoints and then adds the actual custom authentication filter (the logic that validates the request) into the chain.  

**Why this design?**
- By building the chain dynamically in an auto-configuration class, the shared library adapts to whatever environment it is placed in.  

```java
package com.yourcompany.sharedauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SharedAuthProperties.class)
public class SharedAuthAutoConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SharedAuthProperties properties) throws Exception {
        
        http.csrf(csrf -> csrf.disable()); 

        http.authorizeHttpRequests(auth -> {
            if (!properties.getPublicEndpoints().isEmpty()) {
                auth.requestMatchers(properties.getPublicEndpoints().toArray(new String[0])).permitAll();
            }

            if (!properties.getSecuredEndpoints().isEmpty()) {
                auth.requestMatchers(properties.getSecuredEndpoints().toArray(new String[0])).authenticated();
            } else {
                auth.anyRequest().authenticated();
            }
        });

        // Add the actual authentication logic filter
        // http.addFilterBefore(new CustomTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

  
**3.3. Auto-Configuration Registration**  
- Purpose: To tell Spring Boot that this library contains auto-configuration classes.  

**How it works:**
- Create a file at `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` in the shared library and add the fully qualified class name.  

```text
com.yourcompany.sharedauth.config.SharedAuthAutoConfiguration
```

  
**Why is this necessary?** 
- Without this file, Spring Boot's component scan will not look inside the imported JAR file to find the `@Configuration` classes. This file acts as the bridge.  
## 4. Integration Workflow
___

This outlines how a consuming application utilizes the shared library locally. 

**4.1: Local Publishing**  

The shared library must be compiled and published to the local Maven repository (`.m2`).  

- **How:** Run `mvn clean install` in the shared library project.  
- **Why:** This makes the compiled JAR available to other independent projects on the same developer machine. 
    

**4.2: Dependency Injection**  

The consuming application (App A) must declare the starter as a dependency.  

- **How:** Add the following to App A's `pom.xml`.
```xml
<dependency>
    <groupId>com.yourcompany</groupId>
    <artifactId>shared-auth-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

  
**4.3: Local YAML Configuration**  

The consuming application defines its specific routing rules.  

- **How:** Add the parameters to App A's `src/main/resources/application.yml`.
```yaml
shared:
  auth:
    public-endpoints:
      - /api/v1/health
    secured-endpoints:
      - /api/v1/users/**
```

  

- **Why:** This fulfills the requirement that configuration remains completely local to the consuming app. When App A builds, it embeds these specific instructions into the shared library's logic for its own specific runtime context.