package com.aijapanese.speaking.auth.oauth;

import com.aijapanese.speaking.user.entity.OAuthProvider;

/**
 * 인가 코드를 제공자와 교환하고 사용자 정보를 받아 온다.
 *
 * <p>코드가 거절되면 {@link com.aijapanese.speaking.auth.exception.InvalidAuthorizationCodeException},
 * 제공자 오류나 연결 실패면 {@link com.aijapanese.speaking.auth.exception.OAuthProviderException}을 던진다.
 */
public interface SocialProfileClient {

    SocialProfile fetch(OAuthProvider provider, String code, String redirectUri);
}
