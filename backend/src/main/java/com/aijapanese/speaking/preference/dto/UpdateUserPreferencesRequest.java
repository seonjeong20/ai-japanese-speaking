package com.aijapanese.speaking.preference.dto;

import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferencesRequest(
        @NotNull Difficulty defaultDifficulty,
        @NotNull SubtitleMode defaultSubtitleMode
) {
}
