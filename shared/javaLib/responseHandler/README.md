### 1. Goals

- Every app in the monorepo returns responses in one consistent shape (success and error).
- Error definitions (code, message, HTTP status, category) live in a **file**, not hardcoded in Java — so ops/product can tweak messages without a redeploy, and each app can supply its own error catalog on top of shared defaults.
- Zero coupling to any app-specific exception types — shared lib only knows generic contracts.

### 2. Response envelope shape

```json
// Success
{
  "success": true,
  "data": { "id": 1, "name": "..." },
  "timestamp": "2026-07-16T10:00:00Z",
  "traceId": "abc-123"
}

// Error
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "No user found with the given ID",
    "httpStatus": 404,
    "category": "CLIENT_ERROR"
  },
  "timestamp": "2026-07-16T10:00:00Z",
  "traceId": "abc-123"
}
```

### 3. Core classes (shared lib)

```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorPayload error;
    private Instant timestamp;
    private String traceId;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static ApiResponse<?> error(ErrorPayload error) { ... }
}

public class ErrorPayload {
    private String code;
    private String message;
    private int httpStatus;
    private String category;
}
```

No app-specific types imported anywhere here — pure DTOs.

### 4. Error catalog file (externalized, per your requirement)

Ship a **default catalog** inside the shared lib's jar (`classpath:errors/default-errors.yaml`), and let each consuming app point to its **own** file to add/override entries.

```yaml
# errors.yaml — shipped default AND per-app override
errors:
  USER_NOT_FOUND:
    message: "No user found with the given ID"
    httpStatus: 404
    category: CLIENT_ERROR
  VALIDATION_FAILED:
    message: "Request validation failed"
    httpStatus: 400
    category: CLIENT_ERROR
  INTERNAL_ERROR:
    message: "Something went wrong"
    httpStatus: 500
    category: SERVER_ERROR
```

**Load order:** shared defaults loaded first → app-specific file entries merged on top (app entries override by key, new keys just add). This mirrors how you handled `UserDetailsService` — sane default with app override.

### 5. Properties (consumer app's `application.yml`)

```yaml
shared:
  response:
    error-config-path: classpath:errors.yaml   # or file:/etc/config/errors.yaml
    include-trace-id: true
    default-error-code: INTERNAL_ERROR
```

Support both `classpath:` and `file:` prefixes via Spring's `ResourceLoader` — no custom parsing needed for path resolution.
