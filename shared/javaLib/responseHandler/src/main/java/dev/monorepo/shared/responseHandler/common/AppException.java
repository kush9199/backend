package dev.monorepo.shared.responseHandler.common;

import lombok.Getter;

public class AppException extends RuntimeException{
    @Getter
    private final String errorCode;
    public AppException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }
}
