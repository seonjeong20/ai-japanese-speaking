package com.aijapanese.speaking.interview.ai;

import java.util.List;

public record InterviewAnswerCoachingAiResult(
        String coachingSummary,
        List<String> improvementTips,
        String modelAnswer,
        boolean followUpNeeded,
        String followUpReason,
        String followUpQuestion
) {
}
