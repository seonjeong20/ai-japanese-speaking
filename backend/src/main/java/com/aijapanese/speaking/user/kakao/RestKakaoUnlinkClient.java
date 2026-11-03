package com.aijapanese.speaking.user.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * POST https://kapi.kakao.com/v1/user/unlink 를 앱 관리자 키로 부른다.
 * 관리자 키는 로그에 남기지 않는다. 실패하면 예외를 그대로 던지고, 처리는 부르는 쪽이 정한다.
 */
@Component
public class RestKakaoUnlinkClient implements KakaoUnlinkClient {

    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    private final RestClient restClient;
    private final String adminKey;

    public RestKakaoUnlinkClient(RestClient.Builder restClientBuilder, @Value("${kakao.admin-key}") String adminKey) {
        this.restClient = restClientBuilder.build();
        this.adminKey = adminKey;
    }

    @Override
    public void unlink(String kakaoUserId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", kakaoUserId);

        restClient.post()
                .uri(UNLINK_URL)
                .header("Authorization", "KakaoAK " + adminKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
