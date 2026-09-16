package com.aijapanese.speaking.manager.service;

import com.aijapanese.speaking.manager.dto.DailyStudyMinutesResponse;
import com.aijapanese.speaking.manager.dto.ManagerDashboardResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Manager Dashboard/통계 Phase 6 통합 테스트.
 * AI 호출 없이, DB에 저장된 완료(COMPLETED) 세션만으로 Organization 단위 집계가
 * 정확히 계산되는지, 그리고 다른 Organization의 데이터가 섞이지 않는지 검증한다.
 */
@SpringBootTest
@Transactional
class ManagerDashboardServiceIntegrationTest {

    @Autowired private ManagerDashboardService managerDashboardService;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SpeakingSessionRepository speakingSessionRepository;

    @Test
    void getDashboard_aggregatesOnlyCompletedSessionsWithinOwnOrganization() {
        // "now"를 화요일 정오로 고정해 주간 집계(월~일)를 예측 가능하게 만든다.
        LocalDateTime now = fixedTuesdayNoon();
        LocalDate monday = now.toLocalDate().with(DayOfWeek.MONDAY);

        Organization org = activeOrganization("dashboard-agg");
        User manager = createUser(org, "manager-agg@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User learnerA = createUser(org, "learner-agg-a@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        User learnerB = createUser(org, "learner-agg-b@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        createUser(org, "learner-agg-pending@test.com", UserRole.LEARNER, UserStatus.PENDING);

        // 이번 주 월요일: 회화 30분 완료 세션 (learnerA)
        saveCompletedSession(learnerA, SessionType.CONVERSATION, monday.atTime(9, 0), 30 * 60);
        // 이번 주 화요일(오늘): 면접 20분 완료 세션 (learnerB)
        saveCompletedSession(learnerB, SessionType.INTERVIEW, now.toLocalDate().atTime(10, 0), 20 * 60);
        // 진행 중(IN_PROGRESS) 세션 -> 집계에서 제외되어야 함
        SpeakingSession inProgress = new SpeakingSession(learnerA, SessionType.CONVERSATION, SessionStatus.IN_PROGRESS, now, now);
        speakingSessionRepository.save(inProgress);

        // 다른 Organization의 완료 세션 -> 절대 섞이면 안 됨
        Organization otherOrg = activeOrganization("dashboard-agg-other");
        User otherLearner = createUser(otherOrg, "learner-agg-other@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        saveCompletedSession(otherLearner, SessionType.CONVERSATION, monday.atTime(9, 0), 100 * 60);

        ManagerDashboardResponse dashboard = managerDashboardService.getDashboard(manager.getId(), now);

        assertThat(dashboard.learnerCount()).isEqualTo(3); // ACTIVE 2 + PENDING 1 (REJECTED만 제외)
        assertThat(dashboard.pendingLearnerCount()).isEqualTo(1);
        assertThat(dashboard.monthlySpeakingCount()).isEqualTo(1);
        assertThat(dashboard.monthlyInterviewCount()).isEqualTo(1);

        assertThat(dashboard.weeklyUsage()).hasSize(7);
        DailyStudyMinutesResponse mondayUsage = dashboard.weeklyUsage().get(0);
        assertThat(mondayUsage.date()).isEqualTo(monday);
        assertThat(mondayUsage.studyMinutes()).isEqualTo(30);

        DailyStudyMinutesResponse tuesdayUsage = dashboard.weeklyUsage().get(1);
        assertThat(tuesdayUsage.date()).isEqualTo(monday.plusDays(1));
        assertThat(tuesdayUsage.studyMinutes()).isEqualTo(20);

        // 주간 총합 50분 / 소속 학습자 3명 = 16.67분
        assertThat(dashboard.averageStudyMinutes()).isCloseTo(50.0 / 3, within(0.01));
    }

    @Test
    void getDashboard_returnsZeroesForOrganizationWithNoLearnersOrSessions() {
        Organization org = activeOrganization("dashboard-empty");
        User manager = createUser(org, "manager-empty@test.com", UserRole.MANAGER, UserStatus.ACTIVE);

        ManagerDashboardResponse dashboard = managerDashboardService.getDashboard(manager.getId());

        assertThat(dashboard.learnerCount()).isZero();
        assertThat(dashboard.pendingLearnerCount()).isZero();
        assertThat(dashboard.monthlySpeakingCount()).isZero();
        assertThat(dashboard.monthlyInterviewCount()).isZero();
        assertThat(dashboard.averageStudyMinutes()).isZero();
        assertThat(dashboard.weeklyUsage()).hasSize(7);
        assertThat(dashboard.weeklyUsage()).allSatisfy(day -> assertThat(day.studyMinutes()).isZero());
    }

    @Test
    void getDashboard_neverMixesOtherOrganizationLearnerCounts() {
        Organization orgA = activeOrganization("dashboard-isolation-a");
        Organization orgB = activeOrganization("dashboard-isolation-b");
        User managerA = createUser(orgA, "manager-isolation-a@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        createUser(orgA, "learner-isolation-a1@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        createUser(orgB, "learner-isolation-b1@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        createUser(orgB, "learner-isolation-b2@test.com", UserRole.LEARNER, UserStatus.ACTIVE);

        ManagerDashboardResponse dashboard = managerDashboardService.getDashboard(managerA.getId());

        assertThat(dashboard.learnerCount()).isEqualTo(1);
    }

    @Test
    void getDashboard_throwsWhenManagerNotFound() {
        assertThatThrownBy(() -> managerDashboardService.getDashboard(999_999_999L))
                .isInstanceOf(com.aijapanese.speaking.user.exception.UserNotFoundException.class);
    }

    private LocalDateTime fixedTuesdayNoon() {
        LocalDate anchor = LocalDate.now();
        LocalDate tuesday = anchor.with(DayOfWeek.TUESDAY);
        // "이번 주" 화요일이 미래가 되지 않도록, 이미 지난 화요일이면 이번 주 것을 그대로 사용한다.
        return tuesday.atTime(12, 0);
    }

    private void saveCompletedSession(User user, SessionType type, LocalDateTime startedAt, int durationSeconds) {
        SpeakingSession session = new SpeakingSession(user, type, SessionStatus.IN_PROGRESS, startedAt, startedAt);
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
