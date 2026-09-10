package com.aijapanese.speaking.speaking.dto;

import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;

import java.time.LocalDateTime;

public record SpeakingSessionResponse(
        Long sessionId,
        SessionType sessionType,
        SessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer durationSeconds
) {

    public static SpeakingSessionResponse from(SpeakingSession session) {
        return new SpeakingSessionResponse(
                session.getId(),
                session.getSessionType(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDurationSeconds()
        );
    }
}
