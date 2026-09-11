package com.aijapanese.speaking.admin.controller;

import com.aijapanese.speaking.admin.dto.ManagersResponse;
import com.aijapanese.speaking.admin.service.AdminManagerService;
import com.aijapanese.speaking.common.dto.UserStatusChangeResponse;
import com.aijapanese.speaking.user.entity.UserStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/managers")
public class AdminManagerController {

    private final AdminManagerService adminManagerService;

    public AdminManagerController(AdminManagerService adminManagerService) {
        this.adminManagerService = adminManagerService;
    }

    @GetMapping
    public ResponseEntity<ManagersResponse> list(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long organizationId
    ) {
        return ResponseEntity.ok(new ManagersResponse(adminManagerService.getManagers(status, organizationId)));
    }

    @PatchMapping("/{userId}/approve")
    public ResponseEntity<UserStatusChangeResponse> approve(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagerService.approve(userId));
    }

    @PatchMapping("/{userId}/reject")
    public ResponseEntity<UserStatusChangeResponse> reject(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagerService.reject(userId));
    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<UserStatusChangeResponse> deactivate(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagerService.deactivate(userId));
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<UserStatusChangeResponse> activate(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagerService.activate(userId));
    }
}
