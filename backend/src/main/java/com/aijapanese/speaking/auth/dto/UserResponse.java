package com.aijapanese.speaking.auth.dto;

import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;

public record UserResponse(
        Long id,
        String name,
        String email,
        String department,
        UserRole role,
        UserStatus status,
        OrganizationOptionResponse organization
) {

    public static UserResponse from(User user) {
        OrganizationOptionResponse organization = user.getOrganization() == null
                ? null
                : new OrganizationOptionResponse(user.getOrganization().getId(), user.getOrganization().getName());

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getRole(),
                user.getStatus(),
                organization
        );
    }
}
