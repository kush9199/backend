package dev.monorepo.apps.progen.dto.response;

import dev.monorepo.apps.progen.constant.ROLE;

public record SigninResponse(String username, ROLE role, boolean isEnable) {
}
