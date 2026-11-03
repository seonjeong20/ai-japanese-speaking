package com.aijapanese.speaking.auth.exception;

import com.aijapanese.speaking.user.entity.OAuthProvider;

/**
 * 같은 이메일로 다른 제공자 계정이 이미 있다. 409 EMAIL_ALREADY_REGISTERED.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    private final OAuthProvider registeredProvider;

    public EmailAlreadyRegisteredException(OAuthProvider registeredProvider) {
        super("이미 " + displayName(registeredProvider) + "로 가입된 이메일입니다. "
                + displayName(registeredProvider) + "로 로그인해 주세요");
        this.registeredProvider = registeredProvider;
    }

    public OAuthProvider getRegisteredProvider() {
        return registeredProvider;
    }

    private static String displayName(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> "구글";
            case KAKAO -> "카카오";
        };
    }
}
