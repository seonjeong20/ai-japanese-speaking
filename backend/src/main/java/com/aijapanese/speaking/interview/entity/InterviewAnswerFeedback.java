package com.aijapanese.speaking.interview.entity;

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

@Entity
@Table(name = "interview_answer_feedbacks")
public class InterviewAnswerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false, unique = true)
    private InterviewAnswer answer;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "evaluation_summary", columnDefinition = "TEXT")
    private String evaluationSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "json")
    private List<String> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weaknesses", columnDefinition = "json")
    private List<String> weaknesses;

    @Column(name = "coaching_summary", columnDefinition = "TEXT")
    private String coachingSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "improvement_tips", columnDefinition = "json")
    private List<String> improvementTips;

    @Column(name = "improved_answer", columnDefinition = "TEXT")
    private String improvedAnswer;

    @Column(name = "rubric_version", length = 30)
    private String rubricVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", length = 20, nullable = false)
    private GenerationStatus generationStatus;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    protected InterviewAnswerFeedback() {
    }

    public InterviewAnswerFeedback(
            InterviewAnswer answer,
            BigDecimal overallScore,
            String evaluationSummary,
            List<String> strengths,
            List<String> weaknesses,
            String rubricVersion,
            GenerationStatus generationStatus,
            LocalDateTime generatedAt
    ) {
        this.answer = answer;
        this.overallScore = overallScore;
        this.evaluationSummary = evaluationSummary;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.rubricVersion = rubricVersion;
        this.generationStatus = generationStatus;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public InterviewAnswer getAnswer() { return answer; }
    public BigDecimal getOverallScore() { return overallScore; }
    public String getEvaluationSummary() { return evaluationSummary; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getWeaknesses() { return weaknesses; }
    public String getCoachingSummary() { return coachingSummary; }
    public List<String> getImprovementTips() { return improvementTips; }
    public String getImprovedAnswer() { return improvedAnswer; }
    public String getRubricVersion() { return rubricVersion; }
    public GenerationStatus getGenerationStatus() { return generationStatus; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }

    public void applyCoaching(String coachingSummary, List<String> improvementTips, String improvedAnswer) {
        this.coachingSummary = coachingSummary;
        this.improvementTips = improvementTips;
        this.improvedAnswer = improvedAnswer;
    }
}
