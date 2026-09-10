package com.aijapanese.speaking.conversation.entity;

import com.aijapanese.speaking.speaking.entity.SpeakingMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_corrections")
public class ConversationCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private ConversationFeedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = true)
    private SpeakingMessage message;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30, nullable = false)
    private CorrectionCategory category;

    @Column(name = "original_expression", nullable = false, columnDefinition = "TEXT")
    private String originalExpression;

    @Column(name = "suggested_expression", nullable = false, columnDefinition = "TEXT")
    private String suggestedExpression;

    @Column(name = "reading", nullable = true, columnDefinition = "TEXT")
    private String reading;

    @Column(name = "korean_translation", nullable = true, columnDefinition = "TEXT")
    private String koreanTranslation;

    @Column(name = "explanation", nullable = true, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected ConversationCorrection() {
    }

    public ConversationCorrection(
            ConversationFeedback feedback,
            SpeakingMessage message,
            CorrectionCategory category,
            String originalExpression,
            String suggestedExpression,
            String reading,
            String koreanTranslation,
            String explanation,
            int displayOrder
    ) {
        this.feedback = feedback;
        this.message = message;
        this.category = category;
        this.originalExpression = originalExpression;
        this.suggestedExpression = suggestedExpression;
        this.reading = reading;
        this.koreanTranslation = koreanTranslation;
        this.explanation = explanation;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public ConversationFeedback getFeedback() {
        return feedback;
    }

    public SpeakingMessage getMessage() {
        return message;
    }

    public CorrectionCategory getCategory() {
        return category;
    }

    public String getOriginalExpression() {
        return originalExpression;
    }

    public String getSuggestedExpression() {
        return suggestedExpression;
    }

    public String getReading() {
        return reading;
    }

    public String getKoreanTranslation() {
        return koreanTranslation;
    }

    public String getExplanation() {
        return explanation;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
