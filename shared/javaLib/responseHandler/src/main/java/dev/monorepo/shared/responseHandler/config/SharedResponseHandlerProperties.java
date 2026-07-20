package dev.monorepo.shared.responseHandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "shared.response-handler")
public class SharedResponseHandlerProperties {
    private String errorConfigPath;
    private boolean includeTraceId = true;
}
