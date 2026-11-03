package com.aijapanese.speaking.user.controller;

import com.aijapanese.speaking.user.service.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAccountService userAccountService;

    public UserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /**
     * 회원 탈퇴. 계정과 학습 기록을 모두 지운다.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userAccountService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
