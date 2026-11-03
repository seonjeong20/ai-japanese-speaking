package com.aijapanese.speaking.user.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 소셜 로그인 제공자. DB(users.provider)에는 이름 그대로 대문자(GOOGLE, KAKAO)로 저장하고,
 * API(JSON, 경로 변수)에서는 소문자(google, kakao)로 주고받는다.
 */
public enum OAuthProvider {
    GOOGLE,
    KAKAO;

    @JsonValue
    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * API 값(google, kakao)을 enum으로 바꾼다. 모르는 값이면 IllegalArgumentException.
     */
    @JsonCreator
    public static OAuthProvider fromApiValue(String value) {
        if (value != null) {
            for (OAuthProvider provider : values()) {
                if (provider.apiValue().equals(value)) {
                    return provider;
                }
            }
        }
        throw new IllegalArgumentException("지원하지 않는 로그인 제공자입니다: " + value);
    }

    /**
     * 경로 변수 {provider}의 소문자 값(google, kakao)을 OAuthProvider로 바꾼다.
     * 모르는 값이면 변환이 실패해 Spring MVC가 400으로 처리한다.
     */
    @Component
    public static class PathVariableConverter implements Converter<String, OAuthProvider> {

        @Override
        public OAuthProvider convert(String source) {
            return fromApiValue(source);
        }
    }
}
