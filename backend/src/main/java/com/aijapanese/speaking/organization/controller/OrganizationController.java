package com.aijapanese.speaking.organization.controller;

import com.aijapanese.speaking.auth.dto.OrganizationOptionResponse;
import com.aijapanese.speaking.organization.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/signup-options")
    public ResponseEntity<List<OrganizationOptionResponse>> signupOptions() {
        return ResponseEntity.ok(organizationService.getSignupOptions());
    }
}
