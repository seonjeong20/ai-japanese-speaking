package com.aijapanese.speaking.admin.service;

import com.aijapanese.speaking.admin.dto.AdminDailyUsageResponse;
import com.aijapanese.speaking.admin.dto.AdminDashboardResponse;
import com.aijapanese.speaking.admin.dto.RecentOrganizationResponse;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
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
 * Admin Dashboard 통계 Phase 7: 전체 플랫폼(모든 Organization)을 대상으로,
 * 현재 Admin Dashboard 화면(Figma 94:148/98:2/98:211)이 보여주는 지표만 실 데이터로 집계한다.
 * Manager Dashboard(Phase 6)와 동일하게 AI 호출은 하지 않고, 완료(COMPLETED)된
 * Speaking Session과 Organization/User 테이블만을 근거로 계산한다.
 */
@Service
public class AdminDashboardService {

    private static final int RECENT_ORGANIZATION_LIMIT = 4;

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SpeakingSessionRepository speakingSessionRepository;

    public AdminDashboardService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            SpeakingSessionRepository speakingSessionRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.speakingSessionRepository = speakingSessionRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        return getDashboard(LocalDateTime.now());
    }

    /**
     * now를 인자로 받아 테스트에서 시각을 고정할 수 있게 한다.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(LocalDateTime now) {
        long organizationCount = organizationRepository.count();
        long totalUserCount = userRepository.countByRoleIn(List.of(UserRole.LEARNER, UserRole.MANAGER));
        long pendingManagerCount = userRepository.countByRoleAndStatus(UserRole.MANAGER, UserStatus.PENDING);

        LocalDate today = now.toLocalDate();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate rangeStart = monthStart.isBefore(weekStart) ? monthStart : weekStart;

        List<SpeakingSession> sessions = speakingSessionRepository.findCompletedSince(rangeStart.atStartOfDay());

        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        List<SpeakingSession> monthlySessions = sessions.stream()
                .filter(s -> !s.getStartedAt().isBefore(monthStartDateTime))
                .toList();
        long monthlySpeakingCount = monthlySessions.size();
        long monthlyActiveUserCount = monthlySessions.stream()
                .map(s -> s.getUser().getId())
                .distinct()
                .count();

        Map<LocalDate, Long> countByDate = sessions.stream()
                .filter(s -> !s.getStartedAt().toLocalDate().isBefore(weekStart))
                .collect(Collectors.groupingBy(s -> s.getStartedAt().toLocalDate(), Collectors.counting()));

        List<AdminDailyUsageResponse> weeklyUsage = new ArrayList<>();
        long weeklyTotalCount = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            long count = countByDate.getOrDefault(day, 0L);
            weeklyUsage.add(new AdminDailyUsageResponse(day, count));
            weeklyTotalCount += count;
        }

        List<Organization> recentOrganizations = organizationRepository.findAllByOrderByCreatedAtDesc();
        List<RecentOrganizationResponse> recent = recentOrganizations.stream()
                .limit(RECENT_ORGANIZATION_LIMIT)
                .map(organization -> new RecentOrganizationResponse(
                        organization.getId(),
                        organization.getName(),
                        organization.getCreatedAt(),
                        userRepository.countByOrganization_IdAndRoleAndStatusNot(
                                organization.getId(), UserRole.LEARNER, UserStatus.REJECTED)
                ))
                .toList();

        return new AdminDashboardResponse(
                organizationCount,
                totalUserCount,
                monthlySpeakingCount,
                monthlyActiveUserCount,
                pendingManagerCount,
                weeklyUsage,
                weeklyTotalCount,
                recent
        );
    }
}
