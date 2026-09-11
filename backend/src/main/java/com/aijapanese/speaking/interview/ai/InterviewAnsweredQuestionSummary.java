package com.aijapanese.speaking.interview.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * 종합 Feedback 프롬프트를 구성하기 위해 answered 질문 하나를 요약한 값이다.
 * Entity를 AI 계층에 직접 노출하지 않기 위한 뷰 객체다.
 */
public record InterviewAnsweredQuestionSummary(
        int sequenceNo,
        String questionKind,
        String questionText,
        String answerText,
        String evaluationSummary,
        List<String> strengths,
        List<String> weaknesses,
        BigDecimal overallScore
) {
}
