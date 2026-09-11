package com.aijapanese.speaking.manager.controller;

import com.aijapanese.speaking.common.dto.UserStatusChangeResponse;
import com.aijapanese.speaking.manager.dto.ManagedLearnersResponse;
import com.aijapanese.speaking.manager.service.ManagerLearnerService;
import com.aijapanese.speaking.user.entity.UserStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/learners")
public class ManagerLearnerController {

    private final ManagerLearnerService managerLearnerService;

    public ManagerLearnerController(ManagerLearnerService managerLearnerService) {
        this.managerLearnerService = managerLearnerService;
    }

    @GetMapping
    public ResponseEntity<ManagedLearnersResponse> list(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            Authentication authentication
    ) {
        Long managerUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(new ManagedLearnersResponse(
                managerLearnerService.getLearners(managerUserId, status, keyword)
        ));
    }

    @PatchMapping("/{userId}/approve")
    public ResponseEntity<UserStatusChangeResponse> approve(@PathVariable Long userId, Authentication authentication) {
        Long managerUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(managerLearnerService.approve(managerUserId, userId));
    }

    @PatchMapping("/{userId}/reject")
    public ResponseEntity<UserStatusChangeResponse> reject(@PathVariable Long userId, Authentication authentication) {
        Long managerUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(managerLearnerService.reject(managerUserId, userId));
    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<UserStatusChangeResponse> deactivate(@PathVariable Long userId, Authentication authentication) {
        Long managerUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(managerLearnerService.deactivate(managerUserId, userId));
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<UserStatusChangeResponse> activate(@PathVariable Long userId, Authentication authentication) {
        Long managerUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(managerLearnerService.activate(managerUserId, userId));
    }
}
