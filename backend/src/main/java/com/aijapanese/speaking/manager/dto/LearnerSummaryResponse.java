package com.aijapanese.speaking.manager.dto;

import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserStatus;

import java.time.LocalDateTime;

public record LearnerSummaryResponse(
        Long userId,
        String name,
        String email,
        UserStatus status,
        LocalDateTime createdAt,
        long studyCount,
        LocalDateTime lastActivityAt
) {

    public static LearnerSummaryResponse of(User learner, long studyCount, LocalDateTime lastActivityAt) {
        return new LearnerSummaryResponse(
                learner.getId(),
                learner.getName(),
                learner.getEmail(),
                learner.getStatus(),
                learner.getCreatedAt(),
                studyCount,
                lastActivityAt
        );
    }
}
