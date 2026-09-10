package com.aijapanese.speaking.auth.dto;

public record LoginResponse(
        String accessToken,
        UserResponse user
) {
}
