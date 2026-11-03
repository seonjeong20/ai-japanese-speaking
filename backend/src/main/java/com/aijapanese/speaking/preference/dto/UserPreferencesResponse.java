package com.aijapanese.speaking.preference.dto;

import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * OpenAPI UserPreferences. message는 저장(PATCH) 응답에만 있고, 조회 응답에서는 JSON에서 빠진다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserPreferencesResponse(
        Difficulty defaultDifficulty,
        SubtitleMode defaultSubtitleMode,
        String message
) {
}
