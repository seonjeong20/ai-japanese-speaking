package com.aijapanese.speaking.interview.dto;

import java.util.List;

public record InterviewFeedbackResponse(
        Long sessionId,
        InterviewOverallFeedbackResponse overall,
        List<InterviewAnswerFeedbackResponse> answers
) {
}
