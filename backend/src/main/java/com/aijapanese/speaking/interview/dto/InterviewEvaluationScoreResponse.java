package com.aijapanese.speaking.interview.dto;

import com.aijapanese.speaking.interview.entity.EvaluationCriterion;

import java.math.BigDecimal;

public record InterviewEvaluationScoreResponse(
        EvaluationCriterion criterion,
        BigDecimal score,
        String feedback,
        boolean applicable
) {
}
