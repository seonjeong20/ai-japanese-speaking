package com.aijapanese.speaking.interview.dto;

import com.aijapanese.speaking.interview.entity.GenerationStatus;

import java.math.BigDecimal;
import java.util.List;

public record InterviewAnswerFeedbackResponse(
        String question,
        String answer,
        BigDecimal overallScore,
        List<InterviewEvaluationScoreResponse> scores,
        List<String> strengths,
        List<String> weaknesses,
        String coachingSummary,
        List<String> improvementTips,
        String improvedAnswer,
        String rubricVersion,
        GenerationStatus generationStatus
) {
}
