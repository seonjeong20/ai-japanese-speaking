package com.aijapanese.speaking.interview.repository;

import com.aijapanese.speaking.interview.entity.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    boolean existsByQuestion_Id(Long questionId);

    Optional<InterviewAnswer> findByQuestion_Id(Long questionId);
}
