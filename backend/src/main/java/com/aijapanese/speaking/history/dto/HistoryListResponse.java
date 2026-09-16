package com.aijapanese.speaking.history.dto;

import java.util.List;

public record HistoryListResponse(
        List<HistoryListItemResponse> items
) {
}
