package com.aijapanese.speaking.conversation.entity;

import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversation_feedbacks")
public class ConversationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private SpeakingSession session;

    @Column(name = "summary", nullable = true, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "naturalness_comment", nullable = true, columnDefinition = "TEXT")
    private String naturalnessComment;

    @Column(name = "grammar_comment", nullable = true, columnDefinition = "TEXT")
    private String grammarComment;

    @Column(name = "vocabulary_comment", nullable = true, columnDefinition = "TEXT")
    private String vocabularyComment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", nullable = true, columnDefinition = "json")
    private List<String> strengths;

    @Column(name = "next_tip", nullable = true, columnDefinition = "TEXT")
    private String nextTip;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", length = 20, nullable = false)
    private GenerationStatus generationStatus;

    @Column(name = "generated_at", nullable = true)
    private LocalDateTime generatedAt;

    protected ConversationFeedback() {
    }

    public ConversationFeedback(
            SpeakingSession session,
            GenerationStatus generationStatus,
            String summary,
            String naturalnessComment,
            String grammarComment,
            String vocabularyComment,
            List<String> strengths,
            String nextTip,
            LocalDateTime generatedAt
    ) {
        this.session = session;
        this.generationStatus = generationStatus;
        this.summary = summary;
        this.naturalnessComment = naturalnessComment;
        this.grammarComment = grammarComment;
        this.vocabularyComment = vocabularyComment;
        this.strengths = strengths;
        this.nextTip = nextTip;
        this.generatedAt = generatedAt;
    }

    public Long getId() {
        return id;
    }

    public SpeakingSession getSession() {
        return session;
    }

    public String getSummary() {
        return summary;
    }

    public String getNaturalnessComment() {
        return naturalnessComment;
    }

    public String getGrammarComment() {
        return grammarComment;
    }

    public String getVocabularyComment() {
        return vocabularyComment;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public String getNextTip() {
        return nextTip;
    }

    public GenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
