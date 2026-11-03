package com.aijapanese.speaking.auth.exception;

/**
 * 제공자가 이메일을 주지 않았거나 인증되지 않은 이메일이라 가입할 수 없다. 403 EMAIL_NOT_VERIFIED.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("가입할 수 없습니다. 인증된 이메일이 필요합니다");
    }
}
