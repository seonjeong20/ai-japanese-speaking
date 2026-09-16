package com.aijapanese.speaking.admin.service;

import com.aijapanese.speaking.admin.dto.AdminDashboardResponse;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Admin Dashboard(Phase 7) 통합 테스트.
 * 이미 DB에 다른 데이터가 존재할 수 있으므로, 절대값이 아니라 "이 테스트가 만든
 * 데이터로 인해 늘어난 만큼"을 델타로 검증한다 (기존 Manager Dashboard 테스트와
 * 동일한 격리 의도이지만, Admin 집계는 Organization 범위를 두지 않기 때문).
 */
@SpringBootTest
@Transactional
class AdminDashboardServiceIntegrationTest {

    @Autowired private AdminDashboardService adminDashboardService;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SpeakingSessionRepository speakingSessionRepository;

    @Test
    void getDashboard_aggregatesAcrossAllOrganizations() {
        LocalDateTime now = fixedTuesdayNoon();
        LocalDate monday = now.toLocalDate().with(DayOfWeek.MONDAY);

        AdminDashboardResponse before = adminDashboardService.getDashboard(now);

        Organization orgA = activeOrganization("admin-dash-a");
        Organization orgB = activeOrganization("admin-dash-b");
        User learnerA = createUser(orgA, "admin-dash-learner-a@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        User learnerB = createUser(orgB, "admin-dash-learner-b@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        createUser(orgA, "admin-dash-manager-pending@test.com", UserRole.MANAGER, UserStatus.PENDING);

        // 다른 Organization에 걸쳐 완료 세션 2건이 이번 주에 발생 -> Admin 집계는 전체를 합산해야 함
        saveCompletedSession(learnerA, monday.atTime(9, 0), 1200);
        saveCompletedSession(learnerB, now.toLocalDate().atTime(10, 0), 600);
        // 진행 중 세션은 집계 제외
        speakingSessionRepository.save(
                new SpeakingSession(learnerA, SessionType.CONVERSATION, SessionStatus.IN_PROGRESS, now, now));

        AdminDashboardResponse after = adminDashboardService.getDashboard(now);

        assertThat(after.organizationCount() - before.organizationCount()).isEqualTo(2);
        assertThat(after.totalUserCount() - before.totalUserCount()).isEqualTo(3);
        assertThat(after.pendingManagerCount() - before.pendingManagerCount()).isEqualTo(1);
        assertThat(after.monthlySpeakingCount() - before.monthlySpeakingCount()).isEqualTo(2);
        assertThat(after.monthlyActiveUserCount() - before.monthlyActiveUserCount()).isEqualTo(2);
        assertThat(after.weeklyTotalCount() - before.weeklyTotalCount()).isEqualTo(2);
        assertThat(after.weeklyUsage()).hasSize(7);
    }

    @Test
    void getDashboard_returnsRecentOrganizationsOrderedByCreatedAtDesc() {
        Organization older = organizationRepository.save(
                new Organization("Recent-older", OrganizationStatus.ACTIVE, LocalDateTime.now().minusDays(2)));
        Organization newer = organizationRepository.save(
                new Organization("Recent-newer", OrganizationStatus.ACTIVE, LocalDateTime.now()));

        AdminDashboardResponse dashboard = adminDashboardService.getDashboard();

        assertThat(dashboard.recentOrganizations()).isNotEmpty();
        long newerIndex = indexOf(dashboard, newer.getId());
        long olderIndex = indexOf(dashboard, older.getId());
        assertThat(newerIndex).isLessThan(olderIndex);
    }

    @Test
    void getDashboard_handlesEmptyPlatformGracefully() {
        // 별도 데이터를 만들지 않아도 예외 없이 0 이상의 값을 반환해야 한다 (empty-data handling).
        AdminDashboardResponse dashboard = adminDashboardService.getDashboard();

        assertThat(dashboard.organizationCount()).isGreaterThanOrEqualTo(0);
        assertThat(dashboard.weeklyUsage()).hasSize(7);
        assertThat(dashboard.weeklyUsage()).allSatisfy(day -> assertThat(day.sessionCount()).isGreaterThanOrEqualTo(0));
    }

    private long indexOf(AdminDashboardResponse dashboard, Long organizationId) {
        for (int i = 0; i < dashboard.recentOrganizations().size(); i++) {
            if (dashboard.recentOrganizations().get(i).id().equals(organizationId)) {
                return i;
            }
        }
        return Long.MAX_VALUE;
    }

    private LocalDateTime fixedTuesdayNoon() {
        LocalDate anchor = LocalDate.now();
        LocalDate tuesday = anchor.with(DayOfWeek.TUESDAY);
        return tuesday.atTime(12, 0);
    }

    private void saveCompletedSession(User user, LocalDateTime startedAt, int durationSeconds) {
        SpeakingSession session = new SpeakingSession(user, SessionType.CONVERSATION, SessionStatus.IN_PROGRESS, startedAt, startedAt);
        session.complete(startedAt.plusSeconds(durationSeconds));
        speakingSessionRepository.save(session);
    }

    private Organization activeOrganization(String suffix) {
        return organizationRepository.save(new Organization("Org-" + suffix, OrganizationStatus.ACTIVE, LocalDateTime.now()));
    }

    private User createUser(Organization organization, String email, UserRole role, UserStatus status) {
        User user = new User(organization, "Test " + role, email, "hash", null, role, status, LocalDateTime.now());
        return userRepository.save(user);
    }
}
