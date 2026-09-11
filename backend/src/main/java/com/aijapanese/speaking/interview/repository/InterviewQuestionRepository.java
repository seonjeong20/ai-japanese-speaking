package com.aijapanese.speaking.interview.repository;

import com.aijapanese.speaking.interview.entity.InterviewQuestion;
import com.aijapanese.speaking.interview.entity.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findBySession_IdOrderBySequenceNoAsc(Long sessionId);

    boolean existsByParentQuestion_Id(Long parentQuestionId);

    boolean existsBySession_IdAndStatus(Long sessionId, QuestionStatus status);
}
