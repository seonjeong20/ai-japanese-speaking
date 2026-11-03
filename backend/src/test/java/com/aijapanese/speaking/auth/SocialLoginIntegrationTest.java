package com.aijapanese.speaking.auth;

import com.aijapanese.speaking.auth.exception.InvalidAuthorizationCodeException;
import com.aijapanese.speaking.auth.exception.OAuthProviderException;
import com.aijapanese.speaking.auth.oauth.SocialProfile;
import com.aijapanese.speaking.auth.oauth.SocialProfileClient;
import com.aijapanese.speaking.auth.oauth.SpringSocialProfileClient;
import com.aijapanese.speaking.user.entity.OAuthProvider;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/auth/oauth/{provider}. 제공자 통신(SocialProfileClient)은 가짜로 바꾼다.
 * 테스트 이름은 docs/plan.md T3 완료 기준, 괄호의 TC는 docs/specs/account.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SocialLoginIntegrationTest {

    private static final String REDIRECT_URI = "http://localhost:5173/auth/callback/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SocialProfileClient socialProfileClient;

    // TC-1
    @Test
    void googleFirstLogin_createsLearner() throws Exception {
        long before = userRepository.count();
        givenProfile(OAuthProvider.GOOGLE, "code-1",
                new SocialProfile(OAuthProvider.GOOGLE, "g-1", "a@x.com", true, "홍길동"));

        login("google", "code-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.user.provider").value("google"))
                .andExpect(jsonPath("$.user.name").value("홍길동"))
                .andExpect(jsonPath("$.user.email").value("a@x.com"))
                .andExpect(jsonPath("$.user.role").value("LEARNER"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));

        assertThat(userRepository.count()).isEqualTo(before + 1);
        User user = userRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-1").orElseThrow();
        assertThat(user.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(user.getProviderUserId()).isEqualTo("g-1");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getEmail()).isEqualTo("a@x.com");
        assertThat(user.getRole()).isEqualTo(UserRole.LEARNER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // TC-2
    @Test
    void kakaoFirstLogin_createsLearner() throws Exception {
        long before = userRepository.count();
        givenProfile(OAuthProvider.KAKAO, "code-1",
                new SocialProfile(OAuthProvider.KAKAO, "k-1", "b@x.com", true, "카카오사용자"));

        login("kakao", "code-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.user.provider").value("kakao"));

        assertThat(userRepository.count()).isEqualTo(before + 1);
        User user = userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "k-1").orElseThrow();
        assertThat(user.getProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(user.getProviderUserId()).isEqualTo("k-1");
        assertThat(user.getRole()).isEqualTo(UserRole.LEARNER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // TC-3
    @Test
    void returningUser_logsIn_updatesLastLogin() throws Exception {
        User saved = saveUser(OAuthProvider.GOOGLE, "g-1", "홍길동", "a@x.com");
        LocalDateTime oldLogin = LocalDateTime.of(2020, 1, 1, 0, 0);
        saved.updateLastLoginAt(oldLogin);
        userRepository.saveAndFlush(saved);
        long before = userRepository.count();
        givenProfile(OAuthProvider.GOOGLE, "code-1",
                new SocialProfile(OAuthProvider.GOOGLE, "g-1", "a@x.com", true, "홍길동"));

        login("google", "code-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.user.id").value(saved.getId()));

        assertThat(userRepository.count()).isEqualTo(before);
        User user = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(user.getLastLoginAt()).isAfter(oldLogin);
    }

    // TC-4
    @Test
    void returningUser_keepsSignupNameAndEmail() throws Exception {
        User saved = saveUser(OAuthProvider.GOOGLE, "g-1", "홍길동", "a@x.com");
        givenProfile(OAuthProvider.GOOGLE, "code-1",
                new SocialProfile(OAuthProvider.GOOGLE, "g-1", "new@x.com", true, "김길동"));

        login("google", "code-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.user.id").value(saved.getId()))
                .andExpect(jsonPath("$.user.email").value("a@x.com"))
                .andExpect(jsonPath("$.user.name").value("홍길동"));

        User user = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(user.getEmail()).isEqualTo("a@x.com");
        assertThat(user.getName()).isEqualTo("홍길동");
    }

    // TC-5
    @Test
    void googleWithKakaoEmail_is409() throws Exception {
        saveUser(OAuthProvider.KAKAO, "k-1", "카카오사용자", "a@x.com");
        long before = userRepository.count();
        givenProfile(OAuthProvider.GOOGLE, "code-1",
                new SocialProfile(OAuthProvider.GOOGLE, "g-2", "a@x.com", true, "홍길동"));

        login("google", "code-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.registeredProvider").value("kakao"))
                .andExpect(jsonPath("$.message").value("이미 카카오로 가입된 이메일입니다. 카카오로 로그인해 주세요"));

        assertThat(userRepository.count()).isEqualTo(before);
    }

    // TC-6
    @Test
    void kakaoWithGoogleEmail_is409() throws Exception {
        saveUser(OAuthProvider.GOOGLE, "g-1", "홍길동", "a@x.com");
        long before = userRepository.count();
        givenProfile(OAuthProvider.KAKAO, "code-1",
                new SocialProfile(OAuthProvider.KAKAO, "k-2", "a@x.com", true, "카카오사용자"));

        login("kakao", "code-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.registeredProvider").value("google"))
                .andExpect(jsonPath("$.message").value("이미 구글로 가입된 이메일입니다. 구글로 로그인해 주세요"));

        assertThat(userRepository.count()).isEqualTo(before);
    }

    // TC-7
    @Test
    void googleUnverifiedEmail_is403() throws Exception {
        long before = userRepository.count();
        givenProfile(OAuthProvider.GOOGLE, "code-1",
                new SocialProfile(OAuthProvider.GOOGLE, "g-1", "a@x.com", false, "홍길동"));

        expectEmailNotVerified(login("google", "code-1"));
        assertThat(userRepository.count()).isEqualTo(before);
    }

    // TC-8
    @Test
    void kakaoNoEmail_is403() throws Exception {
        long before = userRepository.count();
        givenProfile(OAuthProvider.KAKAO, "code-1",
                new SocialProfile(OAuthProvider.KAKAO, "k-1", null, false, "카카오사용자"));

        expectEmailNotVerified(login("kakao", "code-1"));
        assertThat(userRepository.count()).isEqualTo(before);
    }

    // TC-9
    @Test
    void kakaoUnverifiedEmail_is403() throws Exception {
        long before = userRepository.count();
        givenProfile(OAuthProvider.KAKAO, "code-1",
                new SocialProfile(OAuthProvider.KAKAO, "k-1", "b@x.com", false, "카카오사용자"));

        expectEmailNotVerified(login("kakao", "code-1"));
        assertThat(userRepository.count()).isEqualTo(before);
    }

    // 리뷰 중점 2: 카카오 사용자 정보에 kakao_account 자체가 없거나 is_email_verified가 빠진 경우.
    // 실제 변환(SpringSocialProfileClient.toProfile)을 거친 값으로 500이 아니라 403이어야 한다.
    @Test
    void kakaoWithoutAccountBlock_is403() throws Exception {
        long before = userRepository.count();
        SocialProfile withoutAccount = SpringSocialProfileClient.toProfile(
                OAuthProvider.KAKAO, Map.of("id", 12345L));
        SocialProfile withoutVerifiedFlag = SpringSocialProfileClient.toProfile(
                OAuthProvider.KAKAO, Map.of("id", 12346L, "kakao_account", Map.of("email", "b@x.com")));
        givenProfile(OAuthProvider.KAKAO, "code-1", withoutAccount);
        givenProfile(OAuthProvider.KAKAO, "code-2", withoutVerifiedFlag);

        expectEmailNotVerified(login("kakao", "code-1"));
        expectEmailNotVerified(login("kakao", "code-2"));
        assertThat(userRepository.count()).isEqualTo(before);
    }

    // TC-12
    @Test
    void sameIdDifferentProvider_createsNewAccount() throws Exception {
        User google = saveUser(OAuthProvider.GOOGLE, "g-1", "홍길동", "a@x.com");
        long before = userRepository.count();
        givenProfile(OAuthProvider.KAKAO, "code-1",
                new SocialProfile(OAuthProvider.KAKAO, "g-1", "c@x.com", true, "카카오사용자"));

        login("kakao", "code-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.user.provider").value("kakao"))
                .andExpect(jsonPath("$.user.email").value("c@x.com"));

        assertThat(userRepository.count()).isEqualTo(before + 1);
        User kakao = userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "g-1").orElseThrow();
        assertThat(kakao.getId()).isNotEqualTo(google.getId());
    }

    @Test
    void invalidCode_is400() throws Exception {
        long before = userRepository.count();
        when(socialProfileClient.fetch(any(), anyString(), anyString()))
                .thenThrow(new InvalidAuthorizationCodeException());

        login("google", "bad-code")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUTHORIZATION_CODE"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(userRepository.count()).isEqualTo(before);
    }

    // 리뷰 중점 1: 같은 코드가 두 번 오면 두 번째는 제공자가 거절한다(invalid_grant).
    @Test
    void sameCodeTwice_secondIs400_andOneAccount() throws Exception {
        long before = userRepository.count();
        when(socialProfileClient.fetch(OAuthProvider.GOOGLE, "code-1", REDIRECT_URI + "google"))
                .thenReturn(new SocialProfile(OAuthProvider.GOOGLE, "g-1", "a@x.com", true, "홍길동"))
                .thenThrow(new InvalidAuthorizationCodeException());

        login("google", "code-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(true));
        login("google", "code-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUTHORIZATION_CODE"));

        assertThat(userRepository.count()).isEqualTo(before + 1);
        assertThat(userRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-1")).isPresent();
    }

    @Test
    void providerDown_is502() throws Exception {
        long before = userRepository.count();
        when(socialProfileClient.fetch(any(), anyString(), anyString()))
                .thenThrow(new OAuthProviderException(new RuntimeException("connection refused")));

        login("kakao", "code-1")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("OAUTH_PROVIDER_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void blankCode_is400() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"redirectUri\":\"" + REDIRECT_URI + "google\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUTHORIZATION_CODE"));

        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"redirectUri\":\"" + REDIRECT_URI + "google\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUTHORIZATION_CODE"));

        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code-1\",\"redirectUri\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUTHORIZATION_CODE"));

        verify(socialProfileClient, never()).fetch(any(), any(), any());
    }

    @Test
    void unknownProvider_is400() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code-1\",\"redirectUri\":\"" + REDIRECT_URI + "naver\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(socialProfileClient, never()).fetch(any(), any(), any());
    }

    // 리뷰 수정: 이메일 확인과 저장 사이에 다른 요청이 같은 이메일로 다른 제공자 계정을 먼저 만든 경우.
    // 유일 제약 위반이 500이 아니라 409여야 한다. 실제 커밋이 필요해 테스트 트랜잭션을 쓰지 않고 직접 지운다.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSignupSameEmail_is409() throws Exception {
        String email = "race-" + System.nanoTime() + "@x.com";
        TransactionTemplate otherRequest = new TransactionTemplate(transactionManager);
        otherRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        long before = userRepository.count();
        try {
            givenProfile(OAuthProvider.GOOGLE, "code-1",
                    new SocialProfile(OAuthProvider.GOOGLE, "g-race", email, true, "홍길동"));
            // 첫 save 직전에 다른 요청(새 트랜잭션)이 같은 이메일의 카카오 계정을 먼저 저장·커밋한다.
            // Spring Data 저장소는 JDK 프록시라 callRealMethod를 쓸 수 없어, 스파이의 기본 응답(실제 저장소로 위임)을 쓴다.
            Answer<?> realCall = Mockito.mockingDetails(userRepository).getMockCreationSettings().getDefaultAnswer();
            AtomicBoolean firstSave = new AtomicBoolean(true);
            doAnswer(invocation -> {
                if (firstSave.compareAndSet(true, false)) {
                    otherRequest.executeWithoutResult(status -> userRepository.save(
                            new User(OAuthProvider.KAKAO, "k-race", "카카오사용자", email, LocalDateTime.now())));
                }
                return realCall.answer(invocation);
            }).when(userRepository).save(any(User.class));

            login("google", "code-1")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                    .andExpect(jsonPath("$.registeredProvider").value("kakao"));

            assertThat(userRepository.count()).isEqualTo(before + 1);
            assertThat(userRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-race")).isEmpty();
            assertThat(userRepository.findByEmail(email).orElseThrow().getProvider()).isEqualTo(OAuthProvider.KAKAO);
        } finally {
            otherRequest.executeWithoutResult(status ->
                    userRepository.findByEmail(email).ifPresent(userRepository::delete));
        }
    }

    private void givenProfile(OAuthProvider provider, String code, SocialProfile profile) {
        when(socialProfileClient.fetch(provider, code, REDIRECT_URI + provider.apiValue())).thenReturn(profile);
    }

    private ResultActions login(String provider, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/oauth/" + provider)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"redirectUri\":\"" + REDIRECT_URI + provider + "\"}"));
    }

    private void expectEmailNotVerified(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.message").value("가입할 수 없습니다. 인증된 이메일이 필요합니다"));
    }

    private User saveUser(OAuthProvider provider, String providerUserId, String name, String email) {
        return userRepository.saveAndFlush(new User(provider, providerUserId, name, email, LocalDateTime.now()));
    }
}
