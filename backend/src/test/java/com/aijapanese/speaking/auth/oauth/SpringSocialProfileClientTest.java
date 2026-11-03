package com.aijapanese.speaking.auth.oauth;

import com.aijapanese.speaking.user.entity.OAuthProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사용자 정보 응답 → SocialProfile 변환만 확인한다. 네트워크는 쓰지 않는다.
 */
class SpringSocialProfileClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void kakaoAttributes_mapToProfile() throws Exception {
        // 카카오 GET /v2/user/me 응답 예시(카카오 개발자 문서 형식, 값은 가짜)
        String json = """
                {
                  "id": 123456789,
                  "connected_at": "2026-11-03T00:00:00Z",
                  "kakao_account": {
                    "profile_nickname_needs_agreement": false,
                    "profile": {
                      "nickname": "홍길동",
                      "is_default_nickname": false
                    },
                    "email_needs_agreement": false,
                    "is_email_valid": true,
                    "is_email_verified": true,
                    "email": "b@x.com"
                  }
                }
                """;
        Map<String, Object> attributes = objectMapper.readValue(json, new TypeReference<>() {
        });

        SocialProfile profile = SpringSocialProfileClient.toProfile(OAuthProvider.KAKAO, attributes);

        assertThat(profile.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(profile.providerUserId()).isEqualTo("123456789");
        assertThat(profile.email()).isEqualTo("b@x.com");
        assertThat(profile.emailVerified()).isTrue();
        assertThat(profile.name()).isEqualTo("홍길동");

        // kakao_account가 없으면 빠진 값은 null·false
        SocialProfile withoutAccount = SpringSocialProfileClient.toProfile(
                OAuthProvider.KAKAO, Map.of("id", 123456789L));
        assertThat(withoutAccount.providerUserId()).isEqualTo("123456789");
        assertThat(withoutAccount.email()).isNull();
        assertThat(withoutAccount.emailVerified()).isFalse();
        assertThat(withoutAccount.name()).isNull();
    }
}
