package com.aijapanese.speaking.interview.ai;

/**
 * 첫 면접 질문 생성 결과. 이 단계에서는 평가/점수/피드백/꼬리질문을 다루지 않으므로
 * 질문 텍스트 하나만 구조화한다.
 */
public record InterviewFirstQuestionResult(
        String questionText
) {
}
