package com.aijapanese.speaking.conversation.dto;

import com.aijapanese.speaking.conversation.entity.ConversationCorrection;
import com.aijapanese.speaking.conversation.entity.CorrectionCategory;

public record ConversationCorrectionResponse(
        Long messageId,
        CorrectionCategory category,
        String originalExpression,
        String suggestedExpression,
        String reading,
        String koreanTranslation,
        String explanation,
        int displayOrder
) {

    public static ConversationCorrectionResponse from(ConversationCorrection correction) {
        return new ConversationCorrectionResponse(
                correction.getMessage() != null ? correction.getMessage().getId() : null,
                correction.getCategory(),
                correction.getOriginalExpression(),
                correction.getSuggestedExpression(),
                correction.getReading(),
                correction.getKoreanTranslation(),
                correction.getExplanation(),
                correction.getDisplayOrder()
        );
    }
}
