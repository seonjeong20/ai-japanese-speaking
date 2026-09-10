package com.aijapanese.speaking.conversation.ai;

import com.aijapanese.speaking.conversation.entity.CorrectionCategory;

import java.util.List;

public record ConversationFeedbackAiResult(
        String summary,
        String naturalnessComment,
        String grammarComment,
        String vocabularyComment,
        List<String> strengths,
        String nextTip,
        List<Correction> corrections
) {

    public record Correction(
            CorrectionCategory category,
            String originalExpression,
            String suggestedExpression,
            String reading,
            String koreanTranslation,
            String explanation,
            Integer relatedUserMessageSequenceNo
    ) {
    }
}
