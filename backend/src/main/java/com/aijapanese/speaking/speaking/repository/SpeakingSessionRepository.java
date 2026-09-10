package com.aijapanese.speaking.speaking.repository;

import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {
}
