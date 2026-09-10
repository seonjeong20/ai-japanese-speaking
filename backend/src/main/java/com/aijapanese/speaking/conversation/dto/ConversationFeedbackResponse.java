package com.aijapanese.speaking.conversation.dto;

import com.aijapanese.speaking.conversation.entity.GenerationStatus;

import java.util.List;

public record ConversationFeedbackResponse(
        Long sessionId,
        GenerationStatus generationStatus,
        String summary,
        String naturalnessComment,
        String grammarComment,
        String vocabularyComment,
        List<String> strengths,
        String nextTip,
        List<ConversationCorrectionResponse> corrections
) {
}
