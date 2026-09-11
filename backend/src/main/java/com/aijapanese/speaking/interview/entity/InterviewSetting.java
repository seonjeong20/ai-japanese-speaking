package com.aijapanese.speaking.interview.entity;

import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
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

import java.time.LocalDateTime;

/**
 * 면접 시작 당시 설정. Difficulty/SubtitleMode는 ConversationSetting과 동일한 값 도메인을
 * 공유하므로(OpenAPI에도 하나의 스키마로 정의됨) conversation.entity의 enum을 그대로 재사용한다.
 */
@Entity
@Table(name = "interview_settings")
public class InterviewSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private SpeakingSession session;

    @Column(name = "job_role", length = 100, nullable = false)
    private String jobRole;

    @Column(name = "interview_type", length = 50, nullable = true)
    private String interviewType;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20, nullable = false)
    private Difficulty difficulty;

    @Column(name = "additional_request", nullable = true, columnDefinition = "TEXT")
    private String additionalRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "subtitle_mode", length = 30, nullable = false)
    private SubtitleMode subtitleMode;

    @Column(name = "target_country", length = 30, nullable = true)
    private String targetCountry;

    @Column(name = "target_language", length = 30, nullable = true)
    private String targetLanguage;

    @Column(name = "feedback_language", length = 30, nullable = true)
    private String feedbackLanguage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected InterviewSetting() {
    }

    public InterviewSetting(
            SpeakingSession session,
            String jobRole,
            String interviewType,
            Difficulty difficulty,
            String additionalRequest,
            SubtitleMode subtitleMode,
            String targetCountry,
            String targetLanguage,
            String feedbackLanguage,
            LocalDateTime createdAt
    ) {
        this.session = session;
        this.jobRole = jobRole;
        this.interviewType = interviewType;
        this.difficulty = difficulty;
        this.additionalRequest = additionalRequest;
        this.subtitleMode = subtitleMode;
        this.targetCountry = targetCountry;
        this.targetLanguage = targetLanguage;
        this.feedbackLanguage = feedbackLanguage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public SpeakingSession getSession() {
        return session;
    }

    public String getJobRole() {
        return jobRole;
    }

    public String getInterviewType() {
        return interviewType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getAdditionalRequest() {
        return additionalRequest;
    }

    public SubtitleMode getSubtitleMode() {
        return subtitleMode;
    }

    public String getTargetCountry() {
        return targetCountry;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getFeedbackLanguage() {
        return feedbackLanguage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
