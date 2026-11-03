package com.aijapanese.speaking.user.repository;

import com.aijapanese.speaking.user.entity.OAuthProvider;
import com.aijapanese.speaking.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * users 표의 (provider, provider_user_id) 유일 제약을 실제 PostgreSQL에서 확인한다.
 */
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void providerAndId_isUnique() {
        userRepository.saveAndFlush(user(OAuthProvider.GOOGLE, "g-1", "unique-first@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                user(OAuthProvider.GOOGLE, "g-1", "unique-second@example.com")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    // TC-12의 저장소 부분: 제공자가 다르면 같은 제공자 쪽 ID라도 다른 계정이다.
    @Test
    void sameIdDifferentProvider_isAllowed() {
        User google = userRepository.saveAndFlush(user(OAuthProvider.GOOGLE, "g-1", "same-id-google@example.com"));
        User kakao = userRepository.saveAndFlush(user(OAuthProvider.KAKAO, "g-1", "same-id-kakao@example.com"));

        assertThat(userRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-1"))
                .get().extracting(User::getId).isEqualTo(google.getId());
        assertThat(userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "g-1"))
                .get().extracting(User::getId).isEqualTo(kakao.getId());
    }

    private User user(OAuthProvider provider, String providerUserId, String email) {
        return new User(provider, providerUserId, "Test Learner", email, LocalDateTime.now());
    }
}
