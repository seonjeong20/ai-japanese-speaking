package com.aijapanese.speaking.admin.dto;

import com.aijapanese.speaking.auth.dto.OrganizationOptionResponse;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserStatus;

import java.time.LocalDateTime;

public record ManagerSummaryResponse(
        Long userId,
        String name,
        String email,
        String department,
        OrganizationOptionResponse organization,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {

    public static ManagerSummaryResponse from(User manager) {
        OrganizationOptionResponse organization = new OrganizationOptionResponse(
                manager.getOrganization().getId(),
                manager.getOrganization().getName()
        );
        return new ManagerSummaryResponse(
                manager.getId(),
                manager.getName(),
                manager.getEmail(),
                manager.getDepartment(),
                organization,
                manager.getStatus(),
                manager.getCreatedAt(),
                manager.getLastLoginAt()
        );
    }
}
