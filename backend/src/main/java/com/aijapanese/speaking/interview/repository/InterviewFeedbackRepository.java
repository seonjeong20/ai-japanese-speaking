package com.aijapanese.speaking.interview.repository;

import com.aijapanese.speaking.interview.entity.InterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, Long> {

    Optional<InterviewFeedback> findBySession_Id(Long sessionId);
}
