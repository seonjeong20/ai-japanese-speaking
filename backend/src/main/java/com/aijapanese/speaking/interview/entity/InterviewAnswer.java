package com.aijapanese.speaking.interview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_answers")
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private InterviewQuestion question;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    protected InterviewAnswer() {
    }

    public InterviewAnswer(InterviewQuestion question, String answerText, LocalDateTime submittedAt) {
        this.question = question;
        this.answerText = answerText;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public InterviewQuestion getQuestion() {
        return question;
    }

    public String getAnswerText() {
        return answerText;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
