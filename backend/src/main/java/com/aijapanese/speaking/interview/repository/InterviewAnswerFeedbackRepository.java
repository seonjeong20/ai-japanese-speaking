package com.aijapanese.speaking.interview.repository;

import com.aijapanese.speaking.interview.entity.InterviewAnswerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewAnswerFeedbackRepository extends JpaRepository<InterviewAnswerFeedback, Long> {
    Optional<InterviewAnswerFeedback> findByAnswer_Id(Long answerId);
}
