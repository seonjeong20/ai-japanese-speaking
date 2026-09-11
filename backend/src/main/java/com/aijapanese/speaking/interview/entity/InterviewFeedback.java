package com.aijapanese.speaking.interview.entity;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 면접 전체 종료 후 생성되는 종합 피드백. 질문 하나에 대한 피드백은 InterviewAnswerFeedback을 사용한다.
 */
@Entity
@Table(name = "interview_feedbacks")
public class InterviewFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private SpeakingSession session;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "json")
    private List<String> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "improvements", columnDefinition = "json")
    private List<String> improvements;

    @Column(name = "evaluated_answer_count")
    private Integer evaluatedAnswerCount;

    @Column(name = "rubric_version", length = 30)
    private String rubricVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", length = 20, nullable = false)
    private GenerationStatus generationStatus;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    protected InterviewFeedback() {
    }

    public InterviewFeedback(
            SpeakingSession session,
            BigDecimal overallScore,
            String summary,
            List<String> strengths,
            List<String> improvements,
            Integer evaluatedAnswerCount,
            String rubricVersion,
            GenerationStatus generationStatus,
            LocalDateTime generatedAt
    ) {
        this.session = session;
        this.overallScore = overallScore;
        this.summary = summary;
        this.strengths = strengths;
        this.improvements = improvements;
        this.evaluatedAnswerCount = evaluatedAnswerCount;
        this.rubricVersion = rubricVersion;
        this.generationStatus = generationStatus;
        this.generatedAt = generatedAt;
    }

    public Long getId() {
        return id;
    }

    public SpeakingSession getSession() {
        return session;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public Integer getEvaluatedAnswerCount() {
        return evaluatedAnswerCount;
    }

    public String getRubricVersion() {
        return rubricVersion;
    }

    public GenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
