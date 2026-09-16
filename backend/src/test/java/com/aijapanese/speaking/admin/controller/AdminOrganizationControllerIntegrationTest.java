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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin 기관 관리(Phase 7) API의 Role 기반 접근 제어 및 기본 동작 E2E 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminOrganizationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    void admin_canCreateListRenameAndChangeStatus() throws Exception {
        User admin = createUser(null, "admin-org-crud@test.com", UserRole.ADMIN, UserStatus.ACTIVE);
        String token = jwtTokenProvider.generateToken(admin.getId(), UserRole.ADMIN);
        String name = "Org-" + UUID.randomUUID();

        String createBody = "{\"name\":\"" + name + "\"}";
        String created = mockMvc.perform(post("/api/admin/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        Long organizationId = objectId(created);

        mockMvc.perform(get("/api/admin/organizations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(patch("/api/admin/organizations/" + organizationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "-renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + "-renamed"));

        mockMvc.perform(patch("/api/admin/organizations/" + organizationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void manager_cannotAccessAdminOrganizations() throws Exception {
        Organization org = activeOrganization("admin-org-access-manager");
        User manager = createUser(org, "manager-org-access@test.com", UserRole.MANAGER, UserStatus.ACTIVE);
        String token = jwtTokenProvider.generateToken(manager.getId(), UserRole.MANAGER);

        mockMvc.perform(get("/api/admin/organizations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void learner_cannotAccessAdminOrganizations() throws Exception {
        Organization org = activeOrganization("admin-org-access-learner");
        User learner = createUser(org, "learner-org-access@test.com", UserRole.LEARNER, UserStatus.ACTIVE);
        String token = jwtTokenProvider.generateToken(learner.getId(), UserRole.LEARNER);

        mockMvc.perform(get("/api/admin/organizations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequest_isRejected() throws Exception {
        mockMvc.perform(get("/api/admin/organizations"))
                .andExpect(status().isUnauthorized());
    }

    private Long objectId(String json) {
        int idIndex = json.indexOf("\"id\":");
        String rest = json.substring(idIndex + 5);
        int end = rest.indexOf(',');
        return Long.parseLong(rest.substring(0, end).trim());
    }

    private Organization activeOrganization(String suffix) {
        return organizationRepository.save(new Organization("Org-" + suffix + "-" + UUID.randomUUID(), OrganizationStatus.ACTIVE, LocalDateTime.now()));
    }

    private User createUser(Organization organization, String email, UserRole role, UserStatus status) {
        User user = new User(organization, "Test " + role, email, "hash", null, role, status, LocalDateTime.now());
        return userRepository.save(user);
    }
}
