package dev.monorepo.shared.responseHandler.exception;

import dev.monorepo.shared.responseHandler.common.ApiResponse;
import dev.monorepo.shared.responseHandler.common.AppException;
import dev.monorepo.shared.responseHandler.error.ErrorCatalog;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor
public class SharedExceptionHandler {
    private final ErrorCatalog catalog;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<@NonNull ApiResponse<?>> handleApp(AppException ex) {
        var payload = catalog.resolve(ex.getErrorCode());
        return ResponseEntity
                .status(payload.httpStatus())
                .body(ApiResponse.error(payload));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ApiResponse<?>>  handleUnknown(Exception ex) {
        var payload = catalog.resolve("INTERNAL_ERROR");
        return ResponseEntity
                .status(payload.httpStatus())
                .body(ApiResponse.error(payload));
    }
}
