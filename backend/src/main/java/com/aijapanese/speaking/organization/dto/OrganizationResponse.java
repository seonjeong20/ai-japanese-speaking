package com.aijapanese.speaking.organization.dto;

import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        OrganizationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long learnerCount
) {
    public static OrganizationResponse of(Organization organization, long learnerCount) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt(),
                learnerCount
        );
    }
}
