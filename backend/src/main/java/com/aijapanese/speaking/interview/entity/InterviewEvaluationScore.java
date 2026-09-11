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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(
        name = "interview_evaluation_scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_interview_evaluation_scores_feedback_criterion",
                columnNames = {"answer_feedback_id", "criterion"}
        )
)
public class InterviewEvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_feedback_id", nullable = false)
    private InterviewAnswerFeedback answerFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion", length = 50, nullable = false)
    private EvaluationCriterion criterion;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "applicable", nullable = false)
    private boolean applicable;

    protected InterviewEvaluationScore() {
    }

    public InterviewEvaluationScore(
            InterviewAnswerFeedback answerFeedback,
            EvaluationCriterion criterion,
            BigDecimal score,
            String feedback,
            boolean applicable
    ) {
        this.answerFeedback = answerFeedback;
        this.criterion = criterion;
        this.score = score;
        this.feedback = feedback;
        this.applicable = applicable;
    }

    public Long getId() { return id; }
    public InterviewAnswerFeedback getAnswerFeedback() { return answerFeedback; }
    public EvaluationCriterion getCriterion() { return criterion; }
    public BigDecimal getScore() { return score; }
    public String getFeedback() { return feedback; }
    public boolean isApplicable() { return applicable; }
}
