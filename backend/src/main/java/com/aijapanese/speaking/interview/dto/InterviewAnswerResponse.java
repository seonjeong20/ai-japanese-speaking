package com.aijapanese.speaking.interview.dto;

public record InterviewAnswerResponse(
        Long answerId,
        String answerText,
        InterviewAnswerEvaluationResponse feedback,
        InterviewQuestionResponse nextQuestion,
        boolean isComplete
) {
}
