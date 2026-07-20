package dev.monorepo.shared.responseHandler.config;

import dev.monorepo.shared.responseHandler.common.ErrorPayload;
import dev.monorepo.shared.responseHandler.error.ErrorCatalog;
import dev.monorepo.shared.responseHandler.exception.SharedExceptionHandler;
import dev.monorepo.shared.responseHandler.filter.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(SharedResponseHandlerProperties.class)
public class SharedResponseAutoConfig {

    @Bean
    public ErrorCatalog errorCatalog(
            ResourceLoader loader,
            SharedResponseHandlerProperties props
    ) throws IOException {
        var defaultErrors = YamlErrorLoader.load("classpath:errors/default-errors.yaml", loader);
        var overrideErrors = YamlErrorLoader.load(props.getErrorConfigPath(), loader);
        Map<String, ErrorPayload> merged = new HashMap<>(defaultErrors);
        merged.putAll(overrideErrors);
        return new ErrorCatalog(merged);
    }

    @Bean
    public SharedExceptionHandler sharedExceptionHandler(ErrorCatalog errorCatalog){
        return new SharedExceptionHandler(errorCatalog);
    }

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

}
