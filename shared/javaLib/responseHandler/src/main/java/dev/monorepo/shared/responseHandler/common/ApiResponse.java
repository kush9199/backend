package dev.monorepo.shared.responseHandler.common;

import lombok.*;
import org.slf4j.MDC;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorPayload error;
    private Instant timestamp;
    private String traceId;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        r.timestamp = Instant.now();
        r.traceId = MDC.get("traceId");
        return r;
    }

    public static ApiResponse<?> error(ErrorPayload payload) {
        ApiResponse<Object> r = new ApiResponse<>();
        r.success = false;
        r.error = payload;
        r.timestamp = Instant.now();
        r.traceId = MDC.get("traceId");
        return r;
    }
}
