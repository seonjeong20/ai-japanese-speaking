package com.aijapanese.speaking.speaking.repository;

import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {

    /**
     * 같은 세션에 대한 turn 처리가 동시에 들어와도 speaking_messages의 sequence_no가
     * 겹치지 않도록, 세션당 하나의 turn만 처리되게 행 단위로 잠근다. 이번 MVP에서는
     * 세션이 곧 "한 학습자가 진행 중인 대화 하나"이므로 이 정도 잠금으로 충분하다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SpeakingSession s WHERE s.id = :id")
    Optional<SpeakingSession> findByIdForUpdate(@Param("id") Long id);

    /**
     * My History 목록: 로그인한 LEARNER 본인의 완료된 세션만, 최신순으로 조회한다.
     */
    List<SpeakingSession> findByUser_IdAndStatusOrderByStartedAtDesc(Long userId, SessionStatus status);
}
