package com.aijapanese.speaking.preference.controller;

import com.aijapanese.speaking.preference.dto.UpdateUserPreferencesRequest;
import com.aijapanese.speaking.preference.dto.UserPreferencesResponse;
import com.aijapanese.speaking.preference.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping
    public ResponseEntity<UserPreferencesResponse> get(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userPreferenceService.get(userId));
    }

    @PatchMapping
    public ResponseEntity<UserPreferencesResponse> update(
            Authentication authentication,
            @Valid @RequestBody UpdateUserPreferencesRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userPreferenceService.update(userId, request));
    }
}
