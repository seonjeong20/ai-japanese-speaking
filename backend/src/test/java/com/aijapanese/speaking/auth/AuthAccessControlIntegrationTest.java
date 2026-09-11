package com.aijapanese.speaking.auth;

import com.aijapanese.speaking.auth.dto.LoginRequest;
import com.aijapanese.speaking.auth.dto.SignupRequest;
import com.aijapanese.speaking.auth.dto.SignupRole;
import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth / 가입 승인 / Role 기반 접근 제어 E2E 테스트.
 * 실제 HTTP 계층(Spring Security 필터 체인 포함)을 MockMvc로 통과시켜 검증하며, OpenAI API는 호출하지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Organization activeOrganization(String suffix) {
        return organizationRepository.save(new Organization("Org-" + suffix, OrganizationStatus.ACTIVE, LocalDateTime.now()));
    }

    private Organization inactiveOrganization(String suffix) {
        return organizationRepository.save(new Organization("InactiveOrg-" + suffix, OrganizationStatus.INACTIVE, LocalDateTime.now()));
    }

    private User createUser(Organization organization, String email, UserRole role, UserStatus status) {
        User user = new User(
                organization, "Test " + role, email, passwordEncoder.encode("password123"),
                null, role, status, LocalDateTime.now()
        );
        return userRepository.save(user);
    }

    // 1. LEARNER signup → PENDING
    @Test
    void learnerSignup_resultsInPendingStatus() throws Exception {
        Organization org = activeOrganization("learner-signup");
        SignupRequest request = new SignupRequest("김학습", "learner-signup@test.com", "password123", SignupRole.LEARNER, org.getId(), null);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.role").value("LEARNER"));
    }

    // 2. MANAGER signup → PENDING
    @Test
    void managerSignup_resultsInPendingStatus() throws Exception {
        Organization org = activeOrganization("manager-signup");
        SignupRequest request = new SignupRequest("박매니저", "manager-signup@test.com", "password123", SignupRole.MANAGER, org.getId(), "교육운영팀");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    // 3. ADMIN signup 불가 (SignupRole enum에 ADMIN이 없어 요청 자체가 거부된다)
    @Test
    void adminSignup_isRejected() throws Exception {
        Organization org = activeOrganization("admin-signup");
        String rawBody = """
                {"name":"관리자","email":"admin-signup@test.com","password":"password123","role":"ADMIN","organizationId":%d}
                """.formatted(org.getId());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawBody))
                .andExpect(status().isBadRequest());
    }

    // 4. INACTIVE Organization signup 불가
    @Test
    void signup_rejectsInactiveOrganization() throws Exception {
        Organization org = inactiveOrganization("inactive-signup");
        SignupRequest request = new SignupRequest("최학습", "inactive-org-signup@test.com", "password123", SignupRole.LEARNER, org.getId(), null);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_AVAILABLE"));
    }

    // 5. Manager가 자신의 Learner 승인 가능
    @Test
    void manager_canApproveOwnOrganizationLearner() throws Exception {
        Organization org = activeOrganization("own-approve");
        User manager = createUser(org, "manager-own-approve@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User learner = createUser(org, "learner-own-approve@test.com", UserRole.LEARNER, UserStatus.PENDING);
        String managerToken = jwtTokenProvider.generateToken(manager.getId(), UserRole.MANAGER);

        mockMvc.perform(patch("/api/manager/learners/" + learner.getId() + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // 6. Manager가 다른 Organization Learner 승인 불가
    @Test
    void manager_cannotApproveLearnerFromOtherOrganization() throws Exception {
        Organization org1 = activeOrganization("org1-cross");
        Organization org2 = activeOrganization("org2-cross");
        User manager1 = createUser(org1, "manager1-cross@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User learner2 = createUser(org2, "learner2-cross@test.com", UserRole.LEARNER, UserStatus.PENDING);
        String manager1Token = jwtTokenProvider.generateToken(manager1.getId(), UserRole.MANAGER);

        mockMvc.perform(patch("/api/manager/learners/" + learner2.getId() + "/approve")
                        .header("Authorization", "Bearer " + manager1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // 5b. Manager가 자신의 Organization Learner 목록을 조회할 수 있다 (쿼리 파라미터 없이 호출)
    @Test
    void manager_canListOwnOrganizationLearners() throws Exception {
        Organization org = activeOrganization("list-learners");
        User manager = createUser(org, "manager-list-learners@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        createUser(org, "learner-list-1@test.com", UserRole.LEARNER, UserStatus.PENDING);
        String managerToken = jwtTokenProvider.generateToken(manager.getId(), UserRole.MANAGER);

        mockMvc.perform(get("/api/manager/learners")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("learner-list-1@test.com"))
                .andExpect(jsonPath("$.items[0].studyCount").value(0));
    }

    // 7b. Admin이 전체 Manager 목록을 조회할 수 있다 (쿼리 파라미터 없이 호출)
    @Test
    void admin_canListManagers() throws Exception {
        User admin = createUser(null, "admin-list-managers@test.com", UserRole.ADMIN, UserStatus.ACTIVE);
        Organization org = activeOrganization("list-managers");
        createUser(org, "manager-list-1@test.com", UserRole.MANAGER, UserStatus.PENDING);
        String adminToken = jwtTokenProvider.generateToken(admin.getId(), UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/managers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("manager-list-1@test.com"))
                .andExpect(jsonPath("$.items[0].organization.name").value(org.getName()));
    }

    // 7. Admin이 Manager 승인 가능
    @Test
    void admin_canApproveManager() throws Exception {
        User admin = createUser(null, "admin-approve@test.com", UserRole.ADMIN, UserStatus.ACTIVE);
        Organization org = activeOrganization("admin-approve-org");
        User manager = createUser(org, "manager-to-approve@test.com", UserRole.MANAGER, UserStatus.PENDING);
        String adminToken = jwtTokenProvider.generateToken(admin.getId(), UserRole.ADMIN);

        mockMvc.perform(patch("/api/admin/managers/" + manager.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // 8. Learner가 Manager API 접근 불가
    @Test
    void learner_cannotAccessManagerApi() throws Exception {
        Organization org = activeOrganization("learner-blocked");
        User learner = createUser(org, "learner-blocked@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        String learnerToken = jwtTokenProvider.generateToken(learner.getId(), UserRole.LEARNER);

        mockMvc.perform(get("/api/manager/learners")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isForbidden());
    }

    // 9. Manager가 Admin API 접근 불가
    @Test
    void manager_cannotAccessAdminApi() throws Exception {
        Organization org = activeOrganization("manager-blocked");
        User manager = createUser(org, "manager-blocked@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        String managerToken = jwtTokenProvider.generateToken(manager.getId(), UserRole.MANAGER);

        mockMvc.perform(get("/api/admin/managers")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    // 10. PENDING 사용자는 login 불가
    @Test
    void pendingUser_cannotLogin() throws Exception {
        Organization org = activeOrganization("pending-login");
        createUser(org, "pending-login@test.com", UserRole.LEARNER, UserStatus.PENDING);

        LoginRequest request = new LoginRequest("pending-login@test.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING"));
    }

    // 11. 승인 후 ACTIVE 사용자는 login 가능
    @Test
    void activeUser_canLogin() throws Exception {
        Organization org = activeOrganization("active-login");
        createUser(org, "active-login@test.com", UserRole.LEARNER, UserStatus.ACTIVE);

        LoginRequest request = new LoginRequest("active-login@test.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.status").value(equalTo("ACTIVE")));
    }

    // 12. 잘못된 status transition 거부 (이미 ACTIVE인 학습자를 다시 승인 시도)
    @Test
    void invalidStatusTransition_isRejected() throws Exception {
        Organization org = activeOrganization("invalid-transition");
        User manager = createUser(org, "manager-invalid-transition@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User learner = createUser(org, "learner-invalid-transition@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        String managerToken = jwtTokenProvider.generateToken(manager.getId(), UserRole.MANAGER);

        mockMvc.perform(patch("/api/manager/learners/" + learner.getId() + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }
}
