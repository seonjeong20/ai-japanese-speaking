package com.aijapanese.speaking.interview.service;

import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.interview.ai.InterviewAiService;
import com.aijapanese.speaking.interview.ai.InterviewFirstQuestionResult;
import com.aijapanese.speaking.interview.dto.InterviewStartRequest;
import com.aijapanese.speaking.interview.dto.InterviewStartResponse;
import com.aijapanese.speaking.interview.entity.InterviewSetting;
import com.aijapanese.speaking.interview.entity.QuestionKind;
import com.aijapanese.speaking.interview.entity.QuestionStatus;
import com.aijapanese.speaking.interview.repository.InterviewQuestionRepository;
import com.aijapanese.speaking.interview.repository.InterviewSettingRepository;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Interview Session/Setup/First Question(Phase 1) 통합 테스트.
 * InterviewAiService는 항상 @MockitoBean으로 대체한다 — 실제 OpenAI API를 절대 호출하지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterviewServiceIntegrationTest {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private SpeakingSessionRepository speakingSessionRepository;

    @Autowired
    private InterviewSettingRepository interviewSettingRepository;

    @Autowired
    private InterviewQuestionRepository interviewQuestionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewAiService interviewAiService;

    private Long createUser(String email, UserRole role) {
        Organization organization = organizationRepository.save(
                new Organization("Test Org " + email, OrganizationStatus.ACTIVE, LocalDateTime.now())
        );
        User user = new User(
                organization, "Test " + role, email, passwordEncoder.encode("password123"),
                null, role, UserStatus.ACTIVE, LocalDateTime.now()
        );
        return userRepository.save(user).getId();
    }

    private InterviewStartRequest sampleRequest() {
        return new InterviewStartRequest(
                "Backend Developer", "TECHNICAL", Difficulty.INTERMEDIATE,
                "신입 개발자 수준으로 진행해주세요.", SubtitleMode.JAPANESE_KOREAN
        );
    }

    // 1~6. LEARNER가 Interview Session 생성 가능 / SessionType==INTERVIEW / SessionStatus==IN_PROGRESS
    //      InterviewSetting 정상 저장 / 첫 InterviewQuestion 정상 저장 / 첫 질문이 Response에 포함
    @Test
    void startInterview_createsSessionSettingAndFirstQuestion() {
        Long userId = createUser("interview-start@example.com", UserRole.LEARNER);
        when(interviewAiService.generateFirstQuestion(any()))
                .thenReturn(new InterviewFirstQuestionResult("自己紹介をお願いします。"));

        InterviewStartResponse response = interviewService.startInterview(userId, sampleRequest());

        assertThat(response.sessionId()).isNotNull();
        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(response.firstQuestion()).isNotNull();
        assertThat(response.firstQuestion().questionText()).isEqualTo("自己紹介をお願いします。");
        assertThat(response.firstQuestion().questionKind()).isEqualTo(QuestionKind.INITIAL);
        assertThat(response.firstQuestion().sequenceNo()).isEqualTo(1);

        var session = speakingSessionRepository.findById(response.sessionId()).orElseThrow();
        assertThat(session.getSessionType()).isEqualTo(SessionType.INTERVIEW);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);

        InterviewSetting setting = interviewSettingRepository.findBySession_Id(response.sessionId()).orElseThrow();
        assertThat(setting.getJobRole()).isEqualTo("Backend Developer");
        assertThat(setting.getDifficulty()).isEqualTo(Difficulty.INTERMEDIATE);
        assertThat(setting.getSubtitleMode()).isEqualTo(SubtitleMode.JAPANESE_KOREAN);
        assertThat(setting.getTargetCountry()).isEqualTo("JAPAN");
        assertThat(setting.getTargetLanguage()).isEqualTo("JAPANESE");
        assertThat(setting.getFeedbackLanguage()).isEqualTo("KOREAN");

        var questions = interviewQuestionRepository.findBySession_IdOrderBySequenceNoAsc(response.sessionId());
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getQuestionKind()).isEqualTo(QuestionKind.INITIAL);
        assertThat(questions.get(0).getStatus()).isEqualTo(QuestionStatus.ASKED);
        assertThat(questions.get(0).getParentQuestion()).isNull();
        assertThat(questions.get(0).getSourceAnswerId()).isNull();
    }

    // 7. MANAGER가 Interview Start API 접근 → 403
    @Test
    void manager_cannotAccessInterviewStartApi() throws Exception {
        Long userId = createUser("interview-manager@example.com", UserRole.MANAGER);
        String token = jwtTokenProvider.generateToken(userId, UserRole.MANAGER);

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    // 8. ADMIN이 Interview Start API 접근 → 403
    @Test
    void admin_cannotAccessInterviewStartApi() throws Exception {
        Long userId = createUser("interview-admin@example.com", UserRole.ADMIN);
        String token = jwtTokenProvider.generateToken(userId, UserRole.ADMIN);

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    // 9. AI 첫 질문 생성 실패 시 불완전 Session/Setting/Question이 남지 않음.
    // 서비스 자체의 @Transactional이 실제로 커밋/롤백되는 것을 검증하려면 이 테스트만큼은
    // 클래스 레벨 @Transactional(테스트 트랜잭션 안에서는 자신의 미커밋 write가 항상 보임)에서
    // 벗어나야 하므로 NOT_SUPPORTED로 재정의한다.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void startInterview_leavesNoOrphanDataWhenAiFails() {
        Long userId = createUser("interview-ai-fail@example.com", UserRole.LEARNER);
        Long organizationId = userRepository.findById(userId).orElseThrow().getOrganization().getId();
        when(interviewAiService.generateFirstQuestion(any()))
                .thenThrow(new RuntimeException("simulated AI failure"));

        long sessionCountBefore = speakingSessionRepository.count();
        long settingCountBefore = interviewSettingRepository.count();
        long questionCountBefore = interviewQuestionRepository.count();

        assertThrows(RuntimeException.class, () -> interviewService.startInterview(userId, sampleRequest()));

        assertThat(speakingSessionRepository.count()).isEqualTo(sessionCountBefore);
        assertThat(interviewSettingRepository.count()).isEqualTo(settingCountBefore);
        assertThat(interviewQuestionRepository.count()).isEqualTo(questionCountBefore);

        // 이 테스트는 NOT_SUPPORTED라 자동 롤백되지 않으므로 직접 정리한다.
        userRepository.deleteById(userId);
        organizationRepository.deleteById(organizationId);
    }
}
