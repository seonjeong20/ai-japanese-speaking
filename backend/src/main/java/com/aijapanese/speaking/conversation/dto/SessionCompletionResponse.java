package com.aijapanese.speaking.conversation.dto;

import com.aijapanese.speaking.conversation.entity.GenerationStatus;
import com.aijapanese.speaking.speaking.entity.SessionStatus;

import java.time.LocalDateTime;

public record SessionCompletionResponse(
        Long sessionId,
        SessionStatus status,
        LocalDateTime endedAt,
        Integer durationSeconds,
        GenerationStatus feedbackGenerationStatus
) {
}
