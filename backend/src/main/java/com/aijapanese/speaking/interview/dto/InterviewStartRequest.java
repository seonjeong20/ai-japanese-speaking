package com.aijapanese.speaking.interview.dto;

import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewStartRequest(
        @NotBlank @Size(max = 100) String jobRole,
        @Size(max = 50) String interviewType,
        @NotNull Difficulty difficulty,
        String additionalRequest,
        @NotNull SubtitleMode subtitleMode
) {
}
