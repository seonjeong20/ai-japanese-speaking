package com.aijapanese.speaking.organization.service;

import com.aijapanese.speaking.auth.dto.OrganizationOptionResponse;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationOptionResponse> getSignupOptions() {
        return organizationRepository.findByStatus(OrganizationStatus.ACTIVE).stream()
                .map(organization -> new OrganizationOptionResponse(organization.getId(), organization.getName()))
                .toList();
    }
}
