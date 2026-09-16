package com.aijapanese.speaking.admin.controller;

import com.aijapanese.speaking.organization.dto.ChangeOrganizationStatusRequest;
import com.aijapanese.speaking.organization.dto.CreateOrganizationRequest;
import com.aijapanese.speaking.organization.dto.OrganizationResponse;
import com.aijapanese.speaking.organization.dto.OrganizationsResponse;
import com.aijapanese.speaking.organization.dto.RenameOrganizationRequest;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/organizations")
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    public AdminOrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<OrganizationsResponse> list(@RequestParam(required = false) OrganizationStatus status) {
        return ResponseEntity.ok(new OrganizationsResponse(organizationService.getOrganizations(status)));
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request.name()));
    }

    @PatchMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> rename(
            @PathVariable Long organizationId,
            @Valid @RequestBody RenameOrganizationRequest request
    ) {
        return ResponseEntity.ok(organizationService.rename(organizationId, request.name()));
    }

    @PatchMapping("/{organizationId}/status")
    public ResponseEntity<OrganizationResponse> changeStatus(
            @PathVariable Long organizationId,
            @Valid @RequestBody ChangeOrganizationStatusRequest request
    ) {
        return ResponseEntity.ok(organizationService.changeStatus(organizationId, request.status()));
    }
}
