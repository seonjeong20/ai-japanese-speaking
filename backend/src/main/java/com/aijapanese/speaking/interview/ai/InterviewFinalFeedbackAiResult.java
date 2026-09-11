package com.aijapanese.speaking.interview.ai;

import java.util.List;

/**
 * 면접 전체 종합 결과. 점수는 Backend가 이미 계산해 AI에게 참고 정보로만 제공하므로,
 * AI는 텍스트 종합(summary/strengths/improvements)만 생성한다.
 */
public record InterviewFinalFeedbackAiResult(
        String summary,
        List<String> strengths,
        List<String> improvements
) {
}
