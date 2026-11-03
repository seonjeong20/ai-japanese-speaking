package com.aijapanese.speaking.user.service;

import com.aijapanese.speaking.user.entity.OAuthProvider;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.exception.UserNotFoundException;
import com.aijapanese.speaking.user.kakao.KakaoUnlinkClient;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. 사용자 행을 지우면 DB의 ON DELETE CASCADE가 세션·메시지·피드백·교정·설정·면접 데이터를 함께 지운다.
 */
@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserRepository userRepository;
    private final KakaoUnlinkClient kakaoUnlinkClient;

    public UserAccountService(UserRepository userRepository, KakaoUnlinkClient kakaoUnlinkClient) {
        this.userRepository = userRepository;
        this.kakaoUnlinkClient = kakaoUnlinkClient;
    }

    /**
     * 카카오 가입자면 먼저 연결을 끊는다. 연결 끊기가 실패해도 기록만 남기고 탈퇴를 끝낸다.
     * 구글 가입자는 제공자 토큰을 저장하지 않으므로 연결 끊기를 부르지 않는다.
     */
    @Transactional
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getProvider() == OAuthProvider.KAKAO) {
            try {
                kakaoUnlinkClient.unlink(user.getProviderUserId());
            } catch (RuntimeException ex) {
                log.warn("카카오 연결 끊기 실패, 탈퇴는 계속한다: userId={}, cause={}",
                        userId, ex.getClass().getSimpleName());
            }
        }

        userRepository.delete(user);
    }
}
