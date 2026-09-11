package com.aijapanese.speaking.common.dto;

import com.aijapanese.speaking.user.entity.UserStatus;

public record UserStatusChangeResponse(
        Long userId,
        UserStatus status,
        String message
) {
}
