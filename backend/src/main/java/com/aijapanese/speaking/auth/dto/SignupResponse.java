package com.aijapanese.speaking.auth.dto;

import com.aijapanese.speaking.user.entity.UserStatus;

public record SignupResponse(
        Long userId,
        SignupRole role,
        UserStatus status,
        String message
) {
}
