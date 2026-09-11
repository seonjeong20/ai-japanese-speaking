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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 면접 질문. parent_question_id는 같은 테이블을, source_answer_id는 이 질문을 생성하게 한
 * 이전 답변을 참조한다. 두 관계 모두 DBML에 맞춰 지연 로딩 단방향 관계로 표현한다.
 */
@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SpeakingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id", nullable = true)
    private InterviewQuestion parentQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_answer_id", nullable = true)
    private InterviewAnswer sourceAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_kind", length = 30, nullable = false)
    private QuestionKind questionKind;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "sequence_no", nullable = true)
    private Integer sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QuestionStatus status;

    @Column(name = "intent", nullable = true, columnDefinition = "TEXT")
    private String intent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "core_competencies", nullable = true, columnDefinition = "json")
    private List<String> coreCompetencies;

    @Column(name = "question_type", length = 50, nullable = true)
    private String questionType;

    @Column(name = "star_recommended", nullable = false)
    private boolean starRecommended;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected InterviewQuestion() {
    }

    public InterviewQuestion(
            SpeakingSession session,
            InterviewQuestion parentQuestion,
            InterviewAnswer sourceAnswer,
            QuestionKind questionKind,
            String questionText,
            Integer sequenceNo,
            QuestionStatus status,
            LocalDateTime createdAt
    ) {
        this.session = session;
        this.parentQuestion = parentQuestion;
        this.sourceAnswer = sourceAnswer;
        this.questionKind = questionKind;
        this.questionText = questionText;
        this.sequenceNo = sequenceNo;
        this.status = status;
        this.starRecommended = false;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public SpeakingSession getSession() {
        return session;
    }

    public InterviewQuestion getParentQuestion() {
        return parentQuestion;
    }

    public Long getSourceAnswerId() {
        return sourceAnswer == null ? null : sourceAnswer.getId();
    }

    public InterviewAnswer getSourceAnswer() {
        return sourceAnswer;
    }

    public QuestionKind getQuestionKind() {
        return questionKind;
    }

    public String getQuestionText() {
        return questionText;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public String getIntent() {
        return intent;
    }

    public List<String> getCoreCompetencies() {
        return coreCompetencies;
    }

    public String getQuestionType() {
        return questionType;
    }

    public boolean isStarRecommended() {
        return starRecommended;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void applyAnalysis(
            String intent,
            String questionType,
            List<String> coreCompetencies,
            boolean starRecommended
    ) {
        this.intent = intent;
        this.questionType = questionType;
        this.coreCompetencies = coreCompetencies;
        this.starRecommended = starRecommended;
    }

    public void markAnswered() {
        this.status = QuestionStatus.ANSWERED;
    }
}
