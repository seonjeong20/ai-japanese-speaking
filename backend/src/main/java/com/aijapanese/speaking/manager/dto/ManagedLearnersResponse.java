package com.aijapanese.speaking.manager.dto;

import java.util.List;

public record ManagedLearnersResponse(
        List<LearnerSummaryResponse> items
) {
}
