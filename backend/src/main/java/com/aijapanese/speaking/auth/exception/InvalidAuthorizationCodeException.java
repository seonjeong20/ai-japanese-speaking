package com.aijapanese.speaking.auth.exception;

/**
 * 인가 코드가 없거나 제공자가 코드 교환을 거절했다. 400 INVALID_AUTHORIZATION_CODE.
 */
public class InvalidAuthorizationCodeException extends RuntimeException {

    public InvalidAuthorizationCodeException() {
        super("인가 코드가 올바르지 않습니다. 다시 로그인해 주세요.");
    }
}
