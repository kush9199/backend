package dev.monorepo.shared.responseHandler.error;

import dev.monorepo.shared.responseHandler.common.ErrorPayload;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class ErrorCatalog {
    private final Map<String, ErrorPayload> entries;
    public ErrorPayload resolve(String code) {
        return entries.getOrDefault(code, entries.get("INTERNAL_ERROR"));
    }
}
