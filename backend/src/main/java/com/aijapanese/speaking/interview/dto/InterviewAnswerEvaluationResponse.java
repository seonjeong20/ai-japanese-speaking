package com.aijapanese.speaking.interview.dto;

import com.aijapanese.speaking.interview.entity.GenerationStatus;

import java.math.BigDecimal;
import java.util.List;

public record InterviewAnswerEvaluationResponse(
        BigDecimal overallScore,
        String evaluationSummary,
        List<InterviewEvaluationScoreResponse> scores,
        List<String> strengths,
        List<String> weaknesses,
        String coachingSummary,
        List<String> improvementTips,
        String modelAnswer,
        String rubricVersion,
        GenerationStatus generationStatus,
        boolean starRecommended
) {
}
