package com.aijapanese.speaking.speaking.repository;

import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    @Query("SELECT s.user.id AS userId, COUNT(s) AS sessionCount, MAX(s.startedAt) AS lastActivityAt "
            + "FROM SpeakingSession s WHERE s.user.id IN :userIds GROUP BY s.user.id")
    List<LearnerActivity> findActivityByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * My History 목록: 로그인한 LEARNER 본인의 완료된 세션만, 최신순으로 조회한다.
     */
    List<SpeakingSession> findByUser_IdAndStatusOrderByStartedAtDesc(Long userId, SessionStatus status);

    /**
     * Manager Dashboard 집계용: 같은 Organization 소속 사용자의 완료된 세션을 기간 내에서 조회한다.
     * 월간 집계(월 초부터)와 주간 집계(이번 주 월요일부터)를 한 번에 계산할 수 있도록,
     * 두 기간 중 더 이른 시각부터 지금까지의 완료 세션을 모두 가져와 서비스 레이어에서 분류/집계한다.
     */
    @Query("SELECT s FROM SpeakingSession s WHERE s.user.organization.id = :organizationId "
            + "AND s.status = com.aijapanese.speaking.speaking.entity.SessionStatus.COMPLETED "
            + "AND s.startedAt >= :from")
    List<SpeakingSession> findCompletedByOrganizationSince(
            @Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from
    );

    /**
     * Admin Dashboard 집계용: Organization 구분 없이 플랫폼 전체의 완료된 세션을
     * 기간 내에서 조회한다. Manager Dashboard와 동일하게 월간/주간 집계를 서비스
     * 레이어에서 함께 계산할 수 있도록 더 이른 시각부터 지금까지를 모두 가져온다.
     */
    @Query("SELECT s FROM SpeakingSession s WHERE "
            + "s.status = com.aijapanese.speaking.speaking.entity.SessionStatus.COMPLETED "
            + "AND s.startedAt >= :from")
    List<SpeakingSession> findCompletedSince(@Param("from") LocalDateTime from);

    interface LearnerActivity {
        Long getUserId();

        long getSessionCount();

        LocalDateTime getLastActivityAt();
    }
}
