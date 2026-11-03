package com.aijapanese.speaking.auth.exception;

/**
 * 제공자(구글·카카오) 서버 오류나 연결 실패. 502 OAUTH_PROVIDER_ERROR.
 */
public class OAuthProviderException extends RuntimeException {

    public OAuthProviderException(Throwable cause) {
        super("로그인 제공자와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요.", cause);
    }
}
