package com.aijapanese.speaking.admin.dto;

import java.util.List;

public record ManagersResponse(
        List<ManagerSummaryResponse> items
) {
}
