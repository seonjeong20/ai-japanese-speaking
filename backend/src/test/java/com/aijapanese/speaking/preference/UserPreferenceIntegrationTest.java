package com.aijapanese.speaking.preference;

import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.conversation.entity.ConversationSetting;
import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.conversation.repository.ConversationSettingRepository;
import com.aijapanese.speaking.preference.entity.UserPreference;
import com.aijapanese.speaking.preference.repository.UserPreferenceRepository;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.support.TestUsers;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.kakao.KakaoUnlinkClient;
import com.aijapanese.speaking.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET·PATCH /api/users/me/preferences.
 * 테스트 이름은 docs/plan.md T6 완료 기준, 주석의 TC는 docs/specs/settings.md.
 * 행 수는 flush·clear 뒤 그 사용자의 user_preferences 행을 직접 센다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPreferenceIntegrationTest {

    private static final String URL = "/api/users/me/preferences";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private KakaoUnlinkClient kakaoUnlinkClient;

    @Autowired
    private UserRepository userRepository;
    @MockitoSpyBean
    private UserPreferenceRepository userPreferenceRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private SpeakingSessionRepository speakingSessionRepository;
    @Autowired
    private ConversationSettingRepository conversationSettingRepository;

    // TC-S2
    @Test
    void get_withoutSaved_returnsDefaults_andNoRow() throws Exception {
        User user = TestUsers.learner(userRepository, "pref-default");
        String token = tokenOf(user);

        getPreferences(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDifficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.defaultSubtitleMode").value("JAPANESE_KOREAN"))
                .andExpect(jsonPath("$.message").doesNotExist());
        flushAndClear();

        assertThat(rowCountOf(user.getId())).isZero();
    }

    // TC-S3
    @Test
    void patch_createsRow() throws Exception {
        User user = TestUsers.learner(userRepository, "pref-create");
        String token = tokenOf(user);

        patchPreferences(token, body("ADVANCED", "JAPANESE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDifficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.defaultSubtitleMode").value("JAPANESE"))
                .andExpect(jsonPath("$.message").value("설정이 저장되었습니다."));
        flushAndClear();

        assertThat(rowCountOf(user.getId())).isEqualTo(1);
        UserPreference saved = userPreferenceRepository.findByUser_Id(user.getId()).orElseThrow();
        assertThat(saved.getDefaultDifficulty()).isEqualTo(Difficulty.ADVANCED);
        assertThat(saved.getDefaultSubtitleMode()).isEqualTo(SubtitleMode.JAPANESE);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    // TC-S4
    @Test
    void patch_twice_updatesSameRow() throws Exception {
        User user = TestUsers.learner(userRepository, "pref-twice");
        String token = tokenOf(user);
        patchPreferences(token, body("ADVANCED", "JAPANESE")).andExpect(status().isOk());
        flushAndClear();
        Long firstRowId = userPreferenceRepository.findByUser_Id(user.getId()).orElseThrow().getId();

        patchPreferences(token, body("BEGINNER", "OFF")).andExpect(status().isOk());
        flushAndClear();

        getPreferences(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDifficulty").value("BEGINNER"))
                .andExpect(jsonPath("$.defaultSubtitleMode").value("OFF"));
        assertThat(rowCountOf(user.getId())).isEqualTo(1);
        UserPreference saved = userPreferenceRepository.findByUser_Id(user.getId()).orElseThrow();
        assertThat(saved.getId()).isEqualTo(firstRowId);
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    // TC-S5
    @Test
    void patch_missingOrUnknownValue_is400() throws Exception {
        User user = TestUsers.learner(userRepository, "pref-invalid");
        String token = tokenOf(user);
        patchPreferences(token, body("ADVANCED", "JAPANESE")).andExpect(status().isOk());
        flushAndClear();

        String[] invalidBodies = {
                "{\"defaultSubtitleMode\":\"OFF\"}",
                "{\"defaultDifficulty\":\"BEGINNER\"}",
                "{\"defaultDifficulty\":null,\"defaultSubtitleMode\":\"OFF\"}",
                "{\"defaultDifficulty\":\"EXPERT\",\"defaultSubtitleMode\":\"OFF\"}",
                "{\"defaultDifficulty\":\"BEGINNER\",\"defaultSubtitleMode\":\"KOREAN\"}",
                "{}"
        };
        for (String invalid : invalidBodies) {
            patchPreferences(token, invalid)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        flushAndClear();

        assertThat(rowCountOf(user.getId())).isEqualTo(1);
        UserPreference saved = userPreferenceRepository.findByUser_Id(user.getId()).orElseThrow();
        assertThat(saved.getDefaultDifficulty()).isEqualTo(Difficulty.ADVANCED);
        assertThat(saved.getDefaultSubtitleMode()).isEqualTo(SubtitleMode.JAPANESE);
    }

    // TC-S6
    @Test
    void get_isPerUser() throws Exception {
        User userA = TestUsers.learner(userRepository, "pref-a");
        User userB = TestUsers.learner(userRepository, "pref-b");
        patchPreferences(tokenOf(userA), body("ADVANCED", "OFF")).andExpect(status().isOk());
        flushAndClear();

        getPreferences(tokenOf(userB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDifficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.defaultSubtitleMode").value("JAPANESE_KOREAN"));
        getPreferences(tokenOf(userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDifficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.defaultSubtitleMode").value("OFF"));
        assertThat(rowCountOf(userB.getId())).isZero();
    }

    // TC-S7
    @Test
    void patch_doesNotChangeExistingSessionSetting() throws Exception {
        User user = TestUsers.learner(userRepository, "pref-session");
        LocalDateTime now = LocalDateTime.now();
        SpeakingSession session = speakingSessionRepository.save(
                new SpeakingSession(user, SessionType.CONVERSATION, SessionStatus.IN_PROGRESS, now, now));
        ConversationSetting setting = conversationSettingRepository.save(new ConversationSetting(
                session, "CAFE", "점원", "친절함", "테스트 상황",
                Difficulty.BEGINNER, SubtitleMode.OFF, now));
        flushAndClear();

        patchPreferences(tokenOf(user), body("ADVANCED", "JAPANESE_KOREAN")).andExpect(status().isOk());
        flushAndClear();

        ConversationSetting reloaded = conversationSettingRepository.findById(setting.getId()).orElseThrow();
        assertThat(reloaded.getDifficulty()).isEqualTo(Difficulty.BEGINNER);
        assertThat(reloaded.getSubtitleMode()).isEqualTo(SubtitleMode.OFF);
    }

    // TC-S8
    @Test
    void deleteAccount_removesPreferences() throws Exception {
        User user = TestUsers.learner(userRepository, "pref-delete");
        String token = tokenOf(user);
        patchPreferences(token, body("ADVANCED", "JAPANESE")).andExpect(status().isOk());
        flushAndClear();
        Long preferenceId = userPreferenceRepository.findByUser_Id(user.getId()).orElseThrow().getId();

        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        flushAndClear();

        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(userPreferenceRepository.existsById(preferenceId)).isFalse();
        assertThat(rowCountOf(user.getId())).isZero();
    }

    // TC-S9
    @Test
    void withoutToken_is401() throws Exception {
        User user = TestUsers.learner(userRepository, "pref-no-token");
        flushAndClear();

        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ADVANCED", "JAPANESE")))
                .andExpect(status().isUnauthorized());
        flushAndClear();

        assertThat(rowCountOf(user.getId())).isZero();
    }

    // 리뷰 수정: 첫 저장이 동시에 온 경우. 조회와 저장 사이에 다른 요청이 같은 사용자의 행을 먼저 만들었다.
    // 유일 제약 위반이 500이 아니라, 먼저 만든 행을 이번 값으로 고친 200이어야 한다.
    // 실제 커밋이 필요해 테스트 트랜잭션을 쓰지 않고 마지막에 직접 지운다.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void patch_concurrentFirstSave_updatesExistingRow() throws Exception {
        TransactionTemplate otherRequest = new TransactionTemplate(transactionManager);
        otherRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        User user = otherRequest.execute(status ->
                TestUsers.learner(userRepository, "pref-race-" + System.nanoTime()));
        try {
            // 첫 save 직전에 다른 요청(새 트랜잭션)이 같은 사용자의 행을 BEGINNER/OFF로 먼저 저장·커밋한다.
            // Spring Data 저장소는 JDK 프록시라 callRealMethod를 쓸 수 없어, 스파이의 기본 응답(실제 저장소로 위임)을 쓴다.
            Answer<?> realCall = Mockito.mockingDetails(userPreferenceRepository)
                    .getMockCreationSettings().getDefaultAnswer();
            AtomicBoolean firstSave = new AtomicBoolean(true);
            doAnswer(invocation -> {
                if (firstSave.compareAndSet(true, false)) {
                    otherRequest.executeWithoutResult(status -> userPreferenceRepository.save(new UserPreference(
                            userRepository.getReferenceById(user.getId()),
                            Difficulty.BEGINNER, SubtitleMode.OFF, LocalDateTime.now())));
                }
                return realCall.answer(invocation);
            }).when(userPreferenceRepository).save(any(UserPreference.class));

            patchPreferences(tokenOf(user), body("ADVANCED", "JAPANESE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.defaultDifficulty").value("ADVANCED"))
                    .andExpect(jsonPath("$.defaultSubtitleMode").value("JAPANESE"))
                    .andExpect(jsonPath("$.message").value("설정이 저장되었습니다."));

            assertThat(firstSave.get()).isFalse();
            otherRequest.executeWithoutResult(status -> {
                assertThat(rowCountOf(user.getId())).isEqualTo(1);
                UserPreference saved = userPreferenceRepository.findByUser_Id(user.getId()).orElseThrow();
                assertThat(saved.getDefaultDifficulty()).isEqualTo(Difficulty.ADVANCED);
                assertThat(saved.getDefaultSubtitleMode()).isEqualTo(SubtitleMode.JAPANESE);
                assertThat(saved.getUpdatedAt()).isNotNull();
            });
        } finally {
            // 사용자를 지우면 DB의 ON DELETE CASCADE가 user_preferences 행도 지운다.
            otherRequest.executeWithoutResult(status -> userRepository.deleteById(user.getId()));
        }
    }

    // ---- 도우미 ----

    private ResultActions getPreferences(String token) throws Exception {
        return mockMvc.perform(get(URL).header("Authorization", "Bearer " + token));
    }

    private ResultActions patchPreferences(String token, String json) throws Exception {
        return mockMvc.perform(patch(URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String body(String difficulty, String subtitleMode) {
        return "{\"defaultDifficulty\":\"" + difficulty + "\",\"defaultSubtitleMode\":\"" + subtitleMode + "\"}";
    }

    private String tokenOf(User user) {
        return jwtTokenProvider.generateToken(user.getId(), user.getRole());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private long rowCountOf(Long userId) {
        return entityManager.createQuery(
                        "SELECT COUNT(p) FROM UserPreference p WHERE p.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
