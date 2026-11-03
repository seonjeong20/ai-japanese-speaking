package com.aijapanese.speaking.auth.controller;

import com.aijapanese.speaking.auth.dto.LoginResponse;
import com.aijapanese.speaking.auth.dto.OAuthLoginRequest;
import com.aijapanese.speaking.auth.dto.UserResponse;
import com.aijapanese.speaking.auth.service.AuthService;
import com.aijapanese.speaking.user.entity.OAuthProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 소셜 로그인(가입 겸 로그인). provider는 소문자 google, kakao만 받는다(OAuthProvider.PathVariableConverter).
     */
    @PostMapping("/oauth/{provider}")
    public ResponseEntity<LoginResponse> oauthLogin(
            @PathVariable OAuthProvider provider,
            @RequestBody OAuthLoginRequest request
    ) {
        return ResponseEntity.ok(authService.oauthLogin(provider, request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }
}
