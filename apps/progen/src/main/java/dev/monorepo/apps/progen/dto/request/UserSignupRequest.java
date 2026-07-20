package dev.monorepo.apps.progen.dto.request;

import lombok.With;

@With
public record UserSignupRequest(String username, String password, String email){}
