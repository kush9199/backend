package dev.monorepo.shared.responseHandler.common;

import lombok.With;

@With
public record ErrorPayload (String code, String message, int httpStatus, String category){}
