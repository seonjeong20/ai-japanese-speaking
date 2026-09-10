package com.aijapanese.speaking.conversation.dto;

import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConversationStartRequest(
        @Size(max = 100) String situation,
        @Size(max = 100) String partnerRole,
        @Size(max = 100) String partnerPersonality,
        String situationDescription,
        @NotNull Difficulty difficulty,
        @NotNull SubtitleMode subtitleMode
) {
}
