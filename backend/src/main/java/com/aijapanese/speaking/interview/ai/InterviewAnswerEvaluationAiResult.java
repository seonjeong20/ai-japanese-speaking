package com.aijapanese.speaking.interview.ai;

import com.aijapanese.speaking.interview.entity.EvaluationCriterion;

import java.math.BigDecimal;
import java.util.List;

public record InterviewAnswerEvaluationAiResult(
        String questionIntent,
        String questionType,
        List<String> coreCompetencies,
        boolean starRecommended,
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<CriterionEvaluation> evaluations
) {
    public record CriterionEvaluation(
            EvaluationCriterion criterion,
            BigDecimal score,
            String comment,
            boolean applicable
    ) {
    }
}
