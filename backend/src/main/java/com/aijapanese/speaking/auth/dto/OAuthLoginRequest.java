package com.aijapanese.speaking.auth.dto;

/**
 * 빈 값 검사는 AuthService에서 한다. 계약상 "인가 코드가 없음"도 INVALID_AUTHORIZATION_CODE(400)이기 때문이다.
 */
public record OAuthLoginRequest(
        String code,
        String redirectUri
) {
}
