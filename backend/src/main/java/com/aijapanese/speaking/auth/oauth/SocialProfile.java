package com.aijapanese.speaking.auth.oauth;

import com.aijapanese.speaking.user.entity.OAuthProvider;

/**
 * 제공자(구글·카카오)에게서 받은 사용자 정보. 빠진 값은 null(문자열)·false(인증 여부)로 둔다.
 */
public record SocialProfile(
        OAuthProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String name
) {
}
