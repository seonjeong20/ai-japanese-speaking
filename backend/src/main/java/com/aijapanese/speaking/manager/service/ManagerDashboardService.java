package com.aijapanese.speaking.manager.service;

import com.aijapanese.speaking.manager.dto.DailyStudyMinutesResponse;
import com.aijapanese.speaking.manager.dto.ManagerDashboardResponse;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.exception.UserNotFoundException;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manager Dashboard 통계 Phase 6: 로그인한 Manager가 속한 Organization의
 * 완료(COMPLETED)된 Speaking Session만을 근거로 실 데이터 집계를 제공한다.
 * AI 호출은 전혀 하지 않으며, 다른 Organization의 데이터는 절대 포함하지 않는다.
 */
@Service
public class ManagerDashboardService {

    private final UserRepository userRepository;
    private final SpeakingSessionRepository speakingSessionRepository;

    public ManagerDashboardService(UserRepository userRepository, SpeakingSessionRepository speakingSessionRepository) {
        this.userRepository = userRepository;
        this.speakingSessionRepository = speakingSessionRepository;
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboard(Long managerUserId) {
        return getDashboard(managerUserId, LocalDateTime.now());
    }

    /**
     * now를 인자로 받아 테스트에서 시각을 고정할 수 있게 한다.
     */
    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboard(Long managerUserId, LocalDateTime now) {
        User manager = userRepository.findById(managerUserId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        Long organizationId = manager.getOrganization().getId();

        long learnerCount = userRepository.countByOrganization_IdAndRoleAndStatusNot(
                organizationId, UserRole.LEARNER, UserStatus.REJECTED);
        long pendingLearnerCount = userRepository.countByOrganization_IdAndRoleAndStatus(
                organizationId, UserRole.LEARNER, UserStatus.PENDING);

        LocalDate today = now.toLocalDate();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate rangeStart = monthStart.isBefore(weekStart) ? monthStart : weekStart;

        List<SpeakingSession> sessions = speakingSessionRepository.findCompletedByOrganizationSince(
                organizationId, rangeStart.atStartOfDay());

        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        long monthlySpeakingCount = sessions.stream()
                .filter(s -> !s.getStartedAt().isBefore(monthStartDateTime))
                .filter(s -> s.getSessionType() == SessionType.CONVERSATION)
                .count();
        long monthlyInterviewCount = sessions.stream()
                .filter(s -> !s.getStartedAt().isBefore(monthStartDateTime))
                .filter(s -> s.getSessionType() == SessionType.INTERVIEW)
                .count();

        Map<LocalDate, Integer> minutesByDate = sessions.stream()
                .filter(s -> !s.getStartedAt().toLocalDate().isBefore(weekStart))
                .collect(Collectors.groupingBy(
                        s -> s.getStartedAt().toLocalDate(),
                        Collectors.summingInt(s -> minutesOf(s))
                ));

        List<DailyStudyMinutesResponse> weeklyUsage = new ArrayList<>();
        int weeklyTotalMinutes = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            int minutes = minutesByDate.getOrDefault(day, 0);
            weeklyUsage.add(new DailyStudyMinutesResponse(day, minutes));
            weeklyTotalMinutes += minutes;
        }

        double averageStudyMinutes = learnerCount == 0
                ? 0.0
                : (double) weeklyTotalMinutes / learnerCount;

        return new ManagerDashboardResponse(
                learnerCount,
                monthlySpeakingCount,
                averageStudyMinutes,
                monthlyInterviewCount,
                pendingLearnerCount,
                weeklyUsage
        );
    }

    private int minutesOf(SpeakingSession session) {
        Integer durationSeconds = session.getDurationSeconds();
        if (durationSeconds == null) {
            return 0;
        }
        return durationSeconds / 60;
    }
}
