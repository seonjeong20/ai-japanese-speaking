package com.aijapanese.speaking.organization.service;

import com.aijapanese.speaking.organization.dto.OrganizationResponse;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.exception.DuplicateOrganizationNameException;
import com.aijapanese.speaking.organization.exception.OrganizationNotFoundException;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Admin 기관(Organization) 관리 Phase 7 통합 테스트.
 * 생성/중복 이름 차단/이름 수정/상태 변경(ACTIVE<->INACTIVE)이 기존
 * OrganizationStatus 모델을 그대로 사용해 동작하는지 검증한다.
 * Manager 승인 흐름(AdminManagerService)은 건드리지 않는다.
 */
@SpringBootTest
@Transactional
class OrganizationServiceAdminIntegrationTest {

    @Autowired private OrganizationService organizationService;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void create_savesActiveOrganizationWithZeroLearners() {
        String name = uniqueName("Create-Org");

        OrganizationResponse response = organizationService.create(name);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.status()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(response.learnerCount()).isZero();
    }

    @Test
    void create_rejectsDuplicateNameCaseInsensitively() {
        String name = uniqueName("Dup-Org");
        organizationService.create(name);

        assertThatThrownBy(() -> organizationService.create(name.toUpperCase()))
                .isInstanceOf(DuplicateOrganizationNameException.class);
    }

    @Test
    void rename_updatesNameAndRejectsDuplicateAgainstOtherOrganization() {
        Organization orgA = activeOrganization(uniqueName("Rename-A"));
        Organization orgB = activeOrganization(uniqueName("Rename-B"));

        OrganizationResponse renamed = organizationService.rename(orgA.getId(), uniqueName("Rename-A-New"));
        assertThat(renamed.name()).startsWith("Rename-A-New");

        assertThatThrownBy(() -> organizationService.rename(orgA.getId(), orgB.getName()))
                .isInstanceOf(DuplicateOrganizationNameException.class);
    }

    @Test
    void rename_throwsWhenOrganizationNotFound() {
        assertThatThrownBy(() -> organizationService.rename(999_999_999L, "Anything"))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void changeStatus_togglesBetweenActiveAndInactive() {
        Organization org = activeOrganization(uniqueName("Status-Org"));

        OrganizationResponse deactivated = organizationService.changeStatus(org.getId(), OrganizationStatus.INACTIVE);
        assertThat(deactivated.status()).isEqualTo(OrganizationStatus.INACTIVE);

        OrganizationResponse reactivated = organizationService.changeStatus(org.getId(), OrganizationStatus.ACTIVE);
        assertThat(reactivated.status()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    void changeStatus_toInactive_doesNotDetachOrRemoveAttachedUsers() {
        // Organization에 User가 남아 있어도, 소프트 상태 전환(INACTIVE)은 관계를 끊거나
        // User를 삭제하지 않는다 -> 하드 삭제를 하지 않고 기존 OrganizationStatus 모델을
        // 그대로 재사용하는 것이 이 Phase의 삭제/상태 정책이다.
        Organization org = activeOrganization(uniqueName("Attached-Org"));
        User learner = userRepository.save(
                new User(org, "Learner", uniqueName("learner") + "@test.com", "hash", null,
                        UserRole.LEARNER, UserStatus.ACTIVE, LocalDateTime.now()));

        organizationService.changeStatus(org.getId(), OrganizationStatus.INACTIVE);

        User reloaded = userRepository.findById(learner.getId()).orElseThrow();
        assertThat(reloaded.getOrganization().getId()).isEqualTo(org.getId());
        assertThat(organizationRepository.findById(org.getId())).isPresent();
    }

    @Test
    void getOrganizations_computesLearnerCountExcludingRejected() {
        Organization org = activeOrganization(uniqueName("Learner-Count-Org"));
        userRepository.save(new User(org, "Active Learner", uniqueName("l1") + "@test.com", "hash", null,
                UserRole.LEARNER, UserStatus.ACTIVE, LocalDateTime.now()));
        userRepository.save(new User(org, "Rejected Learner", uniqueName("l2") + "@test.com", "hash", null,
                UserRole.LEARNER, UserStatus.REJECTED, LocalDateTime.now()));

        OrganizationResponse found = organizationService.getOrganizations(null).stream()
                .filter(o -> o.id().equals(org.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(found.learnerCount()).isEqualTo(1);
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private Organization activeOrganization(String name) {
        return organizationRepository.save(new Organization(name, OrganizationStatus.ACTIVE, LocalDateTime.now()));
    }
}
