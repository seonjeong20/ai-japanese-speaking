package com.aijapanese.speaking.support;

import com.aijapanese.speaking.user.entity.OAuthProvider;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.repository.UserRepository;

import java.time.LocalDateTime;

/**
 * 테스트용 사용자를 만든다. 소셜 로그인 흐름을 거치지 않고 저장소에 바로 저장한다.
 */
public final class TestUsers {

    private TestUsers() {
    }

    /**
     * 구글로 가입한 LEARNER(ACTIVE) 하나를 저장해 돌려준다.
     * suffix는 테스트마다 달라야 한다(provider_user_id와 email이 suffix로 정해지고 둘 다 유일 제약이 있다).
     */
    public static User learner(UserRepository userRepository, String suffix) {
        return userRepository.save(new User(
                OAuthProvider.GOOGLE,
                "g-" + suffix,
                "Test Learner",
                "learner-" + suffix + "@example.com",
                LocalDateTime.now()
        ));
    }
}
