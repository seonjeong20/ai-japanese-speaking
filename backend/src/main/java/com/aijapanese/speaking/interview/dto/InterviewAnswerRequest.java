package com.aijapanese.speaking.interview.dto;

import jakarta.validation.constraints.NotBlank;

public record InterviewAnswerRequest(
        @NotBlank String answerText
) {
}
