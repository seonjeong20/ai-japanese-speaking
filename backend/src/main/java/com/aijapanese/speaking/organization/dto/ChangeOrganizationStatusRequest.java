package com.aijapanese.speaking.organization.dto;

import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeOrganizationStatusRequest(
        @NotNull
        OrganizationStatus status
) {
}
