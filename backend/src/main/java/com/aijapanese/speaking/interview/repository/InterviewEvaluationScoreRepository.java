package com.aijapanese.speaking.interview.repository;

import com.aijapanese.speaking.interview.entity.InterviewEvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewEvaluationScoreRepository extends JpaRepository<InterviewEvaluationScore, Long> {
    List<InterviewEvaluationScore> findByAnswerFeedback_IdOrderByIdAsc(Long answerFeedbackId);
}
