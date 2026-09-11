package com.aijapanese.speaking.interview.repository;

import com.aijapanese.speaking.interview.entity.InterviewSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewSettingRepository extends JpaRepository<InterviewSetting, Long> {

    Optional<InterviewSetting> findBySession_Id(Long sessionId);
}
