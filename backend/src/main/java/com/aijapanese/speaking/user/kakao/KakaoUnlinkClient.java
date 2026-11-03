package com.aijapanese.speaking.user.kakao;

/**
 * 카카오 연결 끊기. 탈퇴할 때 카카오 가입자의 앱 연결을 끊는다(앱 관리자 키 사용).
 * 테스트에서는 가짜로 바꾼다.
 */
public interface KakaoUnlinkClient {

    /**
     * @param kakaoUserId 카카오 회원번호(users.provider_user_id)
     */
    void unlink(String kakaoUserId);
}
