package com.aijapanese.speaking.organization.service;

import com.aijapanese.speaking.auth.dto.OrganizationOptionResponse;
import com.aijapanese.speaking.organization.dto.OrganizationResponse;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.exception.DuplicateOrganizationNameException;
import com.aijapanese.speaking.organization.exception.OrganizationNotFoundException;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public OrganizationService(OrganizationRepository organizationRepository, UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationOptionResponse> getSignupOptions() {
        return organizationRepository.findByStatus(OrganizationStatus.ACTIVE).stream()
                .map(organization -> new OrganizationOptionResponse(organization.getId(), organization.getName()))
                .toList();
    }

    /**
     * Admin 기관 관리 목록: 상태 필터는 선택이며, 학습자 수는 Manager Dashboard와 동일하게
     * 가입 승인이 거절(REJECTED)되지 않은 LEARNER 수로 계산한다(파생값, 저장 필드 아님).
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getOrganizations(OrganizationStatus status) {
        List<Organization> organizations = status == null
                ? organizationRepository.findAllByOrderByCreatedAtDesc()
                : organizationRepository.findByStatus(status);
        return organizations.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(organization -> OrganizationResponse.of(organization, learnerCountOf(organization)))
                .toList();
    }

    @Transactional
    public OrganizationResponse create(String name) {
        String trimmed = name.trim();
        if (organizationRepository.existsByNameIgnoreCase(trimmed)) {
            throw new DuplicateOrganizationNameException("이미 등록된 기관입니다.");
        }
        Organization organization = new Organization(trimmed, OrganizationStatus.ACTIVE, LocalDateTime.now());
        organizationRepository.save(organization);
        return OrganizationResponse.of(organization, 0L);
    }

    @Transactional
    public OrganizationResponse rename(Long organizationId, String name) {
        Organization organization = getOrganization(organizationId);
        String trimmed = name.trim();
        if (organizationRepository.existsByNameIgnoreCaseAndIdNot(trimmed, organizationId)) {
            throw new DuplicateOrganizationNameException("이미 등록된 기관입니다.");
        }
        organization.rename(trimmed, LocalDateTime.now());
        return OrganizationResponse.of(organization, learnerCountOf(organization));
    }

    @Transactional
    public OrganizationResponse changeStatus(Long organizationId, OrganizationStatus status) {
        Organization organization = getOrganization(organizationId);
        organization.changeStatus(status, LocalDateTime.now());
        return OrganizationResponse.of(organization, learnerCountOf(organization));
    }

    private Organization getOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("기관을 찾을 수 없습니다."));
    }

    private long learnerCountOf(Organization organization) {
        return userRepository.countByOrganization_IdAndRoleAndStatusNot(
                organization.getId(), UserRole.LEARNER, UserStatus.REJECTED);
    }
}
