package com.aijapanese.speaking.manager.controller;

import com.aijapanese.speaking.manager.dto.ManagerDashboardResponse;
import com.aijapanese.speaking.manager.service.ManagerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/dashboard")
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    public ManagerDashboardController(ManagerDashboardService managerDashboardService) {
        this.managerDashboardService = managerDashboardService;
    }

    @GetMapping
    public ResponseEntity<ManagerDashboardResponse> getDashboard(Authentication authentication) {
        Long managerUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(managerDashboardService.getDashboard(managerUserId));
    }
}
