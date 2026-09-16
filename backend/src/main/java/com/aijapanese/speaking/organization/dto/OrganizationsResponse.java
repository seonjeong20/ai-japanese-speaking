package com.aijapanese.speaking.organization.dto;

import java.util.List;

public record OrganizationsResponse(
        List<OrganizationResponse> items
) {
}
