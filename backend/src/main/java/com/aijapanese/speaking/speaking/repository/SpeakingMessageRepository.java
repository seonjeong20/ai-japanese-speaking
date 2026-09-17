package com.aijapanese.speaking.speaking.repository;

import com.aijapanese.speaking.speaking.entity.SpeakingMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpeakingMessageRepository extends JpaRepository<SpeakingMessage, Long> {

    long countBySession_Id(Long sessionId);

    List<SpeakingMessage> findTop10BySession_IdOrderBySequenceNoDesc(Long sessionId);

    List<SpeakingMessage> findBySession_IdOrderBySequenceNoAsc(Long sessionId);

    /**
     * 세션의 첫 메시지(sequence_no 최소값)를 조회한다.
     * "AI opening이 이미 생성되었는가"는 메시지가 존재하는지가 아니라
     * 이 첫 메시지가 AI 발화인지로 판단해야 한다 (countBySession_Id > 0 은 이 판단에 부적절하다).
     */
    Optional<SpeakingMessage> findFirstBySession_IdOrderBySequenceNoAsc(Long sessionId);
}
