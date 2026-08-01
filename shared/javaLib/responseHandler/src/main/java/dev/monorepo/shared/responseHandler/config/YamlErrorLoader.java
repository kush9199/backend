package dev.monorepo.shared.responseHandler.config;

import dev.monorepo.shared.responseHandler.common.ErrorPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class YamlErrorLoader {
    public static Map<String, ErrorPayload> load(String path, ResourceLoader loader) {
        if (path == null) return Map.of();

        try {
            var resource = loader.getResource(path);
            if (!resource.exists()) return Map.of();

            try (var in = resource.getInputStream()) {
                var yaml = new Yaml();
                Map<String, Object> root = yaml.load(in);
                if (root == null || !root.containsKey("errors")) return Map.of();

                var rawErrors = (Map<String, Map<String, Object>>) root.get("errors");
                if (rawErrors == null) return Map.of();

                Map<String, ErrorPayload> result = new HashMap<>();

                for (var entry : rawErrors.entrySet()) {
                    String code = entry.getKey();
                    var v = entry.getValue();

                    String message = v != null ? (String) v.get("message") : null;
                    Object statusObj = v != null ? v.get("httpStatus") : null;
                    String category = v != null ? (String) v.get("category") : null;

                    if (message == null || message.isBlank() ||
                            statusObj == null ||
                            category == null || category.isBlank()) {
                        throw new IllegalStateException("Malformed error config for key: " + code);
                    }

                    int httpStatus = ((Number) statusObj).intValue();

                    result.put(code, new ErrorPayload(
                            code,
                            message,
                            httpStatus,
                            category
                    ));
                }

                log.info("Successfully loaded {} error definitions", result.size());
                return result;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load error config: " + path, ex);
        }
    }
}
