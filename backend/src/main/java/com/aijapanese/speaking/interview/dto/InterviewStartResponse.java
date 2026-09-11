package com.aijapanese.speaking.interview.dto;

import com.aijapanese.speaking.speaking.entity.SessionStatus;

public record InterviewStartResponse(
        Long sessionId,
        SessionStatus status,
        InterviewQuestionResponse firstQuestion
) {
}
