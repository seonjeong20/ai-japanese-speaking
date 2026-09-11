package com.aijapanese.speaking.interview.dto;

import com.aijapanese.speaking.interview.entity.InterviewQuestion;
import com.aijapanese.speaking.interview.entity.QuestionKind;

public record InterviewQuestionResponse(
        Long questionId,
        QuestionKind questionKind,
        String questionText,
        Integer sequenceNo
) {

    public static InterviewQuestionResponse from(InterviewQuestion question) {
        return new InterviewQuestionResponse(
                question.getId(),
                question.getQuestionKind(),
                question.getQuestionText(),
                question.getSequenceNo()
        );
    }
}
