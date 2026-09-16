package com.aijapanese.speaking.admin.dto;

import java.time.LocalDateTime;

public record RecentOrganizationResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        long learnerCount
) {
}
