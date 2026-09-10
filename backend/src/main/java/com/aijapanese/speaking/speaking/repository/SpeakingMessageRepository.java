package com.aijapanese.speaking.speaking.repository;

import com.aijapanese.speaking.speaking.entity.SpeakingMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpeakingMessageRepository extends JpaRepository<SpeakingMessage, Long> {

    long countBySession_Id(Long sessionId);

    List<SpeakingMessage> findTop10BySession_IdOrderBySequenceNoDesc(Long sessionId);

    List<SpeakingMessage> findBySession_IdOrderBySequenceNoAsc(Long sessionId);
}
