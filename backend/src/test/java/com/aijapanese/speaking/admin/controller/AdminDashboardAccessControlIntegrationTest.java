package com.aijapanese.speaking.admin.controller;

import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin Dashboard(Phase 7) API의 Role 기반 접근 제어 E2E 테스트.
 * Manager Dashboard 접근 제어 테스트와 동일한 방식으로, 실제 Security 필터 체인을 통과시킨다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminDashboardAccessControlIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    void admin_canViewDashboard() throws Exception {
        User admin = createUser(null, "admin-dashboard-view@test.com", UserRole.ADMIN, UserStatus.ACTIVE);
        String token = jwtTokenProvider.generateToken(admin.getId(), UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyUsage").isArray())
                .andExpect(jsonPath("$.recentOrganizations").isArray());
    }

    @Test
    void manager_cannotAccessAdminDashboard() throws Exception {
        Organization org = activeOrganization("admin-dash-access-manager");
        User manager = createUser(org, "manager-dashboard-view@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        String token = jwtTokenProvider.generateToken(manager.getId(), UserRole.MANAGER);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void learner_cannotAccessAdminDashboard() throws Exception {
        Organization org = activeOrganization("admin-dash-access-learner");
        User learner = createUser(org, "learner-dashboard-view@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        String token = jwtTokenProvider.generateToken(learner.getId(), UserRole.LEARNER);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequest_isRejected() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    private Organization activeOrganization(String suffix) {
        return organizationRepository.save(new Organization("Org-" + suffix, OrganizationStatus.ACTIVE, LocalDateTime.now()));
    }

    private User createUser(Organization organization, String email, UserRole role, UserStatus status) {
        User user = new User(organization, "Test " + role, email, "hash", null, role, status, LocalDateTime.now());
        return userRepository.save(user);
    }
}
