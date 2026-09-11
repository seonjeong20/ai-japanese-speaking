package com.aijapanese.speaking.interview.dto;

import com.aijapanese.speaking.interview.entity.GenerationStatus;

import java.math.BigDecimal;
import java.util.List;

public record InterviewOverallFeedbackResponse(
        BigDecimal overallScore,
        String summary,
        List<String> strengths,
        List<String> improvements,
        Integer evaluatedAnswerCount,
        String rubricVersion,
        GenerationStatus generationStatus
) {
}
