package com.aijapanese.speaking.auth.dto;

import com.aijapanese.speaking.user.entity.OAuthProvider;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;

public record UserResponse(
        Long id,
        String name,
        String email,
        OAuthProvider provider,
        UserRole role,
        UserStatus status
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProvider(),
                user.getRole(),
                user.getStatus()
        );
    }
}
