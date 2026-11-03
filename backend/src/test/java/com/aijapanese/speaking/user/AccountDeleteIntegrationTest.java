package com.aijapanese.speaking.user;

import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.conversation.entity.ConversationCorrection;
import com.aijapanese.speaking.conversation.entity.ConversationFeedback;
import com.aijapanese.speaking.conversation.entity.ConversationSetting;
import com.aijapanese.speaking.conversation.entity.CorrectionCategory;
import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.conversation.repository.ConversationCorrectionRepository;
import com.aijapanese.speaking.conversation.repository.ConversationFeedbackRepository;
import com.aijapanese.speaking.conversation.repository.ConversationSettingRepository;
import com.aijapanese.speaking.interview.entity.EvaluationCriterion;
import com.aijapanese.speaking.interview.entity.InterviewAnswer;
import com.aijapanese.speaking.interview.entity.InterviewAnswerFeedback;
import com.aijapanese.speaking.interview.entity.InterviewEvaluationScore;
import com.aijapanese.speaking.interview.entity.InterviewFeedback;
import com.aijapanese.speaking.interview.entity.InterviewQuestion;
import com.aijapanese.speaking.interview.entity.InterviewSetting;
import com.aijapanese.speaking.interview.entity.QuestionKind;
import com.aijapanese.speaking.interview.entity.QuestionStatus;
import com.aijapanese.speaking.interview.repository.InterviewAnswerFeedbackRepository;
import com.aijapanese.speaking.interview.repository.InterviewAnswerRepository;
import com.aijapanese.speaking.interview.repository.InterviewEvaluationScoreRepository;
import com.aijapanese.speaking.interview.repository.InterviewFeedbackRepository;
import com.aijapanese.speaking.interview.repository.InterviewQuestionRepository;
import com.aijapanese.speaking.interview.repository.InterviewSettingRepository;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.Speaker;
import com.aijapanese.speaking.speaking.entity.SpeakingMessage;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import com.aijapanese.speaking.speaking.repository.SpeakingMessageRepository;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.support.TestUsers;
import com.aijapanese.speaking.user.entity.OAuthProvider;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.kakao.KakaoUnlinkClient;
import com.aijapanese.speaking.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DELETE /api/users/me. 카카오 연결 끊기(KakaoUnlinkClient)는 가짜로 바꾼다.
 * 테스트 이름은 docs/plan.md T4 완료 기준, 괄호의 TC는 docs/specs/account.md.
 * 지우는 일은 DB의 ON DELETE CASCADE가 하므로, 삭제 뒤 flush·clear하고 저장소로 남은 행을 다시 센다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountDeleteIntegrationTest {

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
    @Autowired
    private SpeakingSessionRepository speakingSessionRepository;
    @Autowired
    private SpeakingMessageRepository speakingMessageRepository;
    @Autowired
    private ConversationSettingRepository conversationSettingRepository;
    @Autowired
    private ConversationFeedbackRepository conversationFeedbackRepository;
    @Autowired
    private ConversationCorrectionRepository conversationCorrectionRepository;
    @Autowired
    private InterviewSettingRepository interviewSettingRepository;
    @Autowired
    private InterviewQuestionRepository interviewQuestionRepository;
    @Autowired
    private InterviewAnswerRepository interviewAnswerRepository;
    @Autowired
    private InterviewAnswerFeedbackRepository interviewAnswerFeedbackRepository;
    @Autowired
    private InterviewEvaluationScoreRepository interviewEvaluationScoreRepository;
    @Autowired
    private InterviewFeedbackRepository interviewFeedbackRepository;

    // TC-14
    @Test
    void delete_removesUserAndAllLearningData() throws Exception {
        User user = TestUsers.learner(userRepository, "1");
        LearningData data = createLearningData(user);
        String token = tokenOf(user);
        flushAndClear();

        deleteMe(token).andExpect(status().isNoContent());
        flushAndClear();

        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(data.countExisting()).isZero();
        verifyNoInteractions(kakaoUnlinkClient);
    }

    // 리뷰 중점 4
    @Test
    void delete_keepsOtherUsersInterviewData() throws Exception {
        User leaving = TestUsers.learner(userRepository, "delete-leaving");
        User staying = TestUsers.learner(userRepository, "delete-staying");
        createLearningData(leaving);
        LearningData stayingData = createLearningData(staying);
        int stayingRows = stayingData.size();
        String token = tokenOf(leaving);
        flushAndClear();

        deleteMe(token).andExpect(status().isNoContent());
        flushAndClear();

        assertThat(userRepository.existsById(staying.getId())).isTrue();
        assertThat(stayingData.countExisting()).isEqualTo(stayingRows);
        assertThat(speakingSessionRepository.findAllById(stayingData.sessionIds)).hasSize(2);
        assertThat(interviewQuestionRepository.findAllById(stayingData.questionIds)).hasSize(2);
        assertThat(interviewAnswerRepository.findAllById(stayingData.answerIds)).hasSize(2);
    }

    // TC-15
    @Test
    void oldTokenAfterDelete_is401() throws Exception {
        User user = TestUsers.learner(userRepository, "delete-old-token");
        String token = tokenOf(user);

        deleteMe(token).andExpect(status().isNoContent());
        flushAndClear();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // TC-16
    @Test
    void rejoinAfterDelete_createsNewAccount() throws Exception {
        User user = TestUsers.learner(userRepository, "1");
        createLearningData(user);
        String token = tokenOf(user);
        flushAndClear();

        deleteMe(token).andExpect(status().isNoContent());
        flushAndClear();

        User rejoined = userRepository.saveAndFlush(new User(
                OAuthProvider.GOOGLE, "g-1", "Test Learner", user.getEmail(), LocalDateTime.now()
        ));
        flushAndClear();

        assertThat(rejoined.getId()).isNotEqualTo(user.getId());
        assertThat(userRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-1"))
                .get().extracting(User::getId).isEqualTo(rejoined.getId());
        assertThat(sessionCountOf(rejoined.getId())).isZero();
    }

    // TC-19
    @Test
    void kakaoDelete_callsUnlinkWithMemberId() throws Exception {
        User user = kakaoUser();
        String token = tokenOf(user);

        deleteMe(token).andExpect(status().isNoContent());
        flushAndClear();

        verify(kakaoUnlinkClient, times(1)).unlink("k-1");
        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    // TC-18
    @Test
    void kakaoUnlinkFails_stillDeletes() throws Exception {
        User user = kakaoUser();
        LearningData data = createLearningData(user);
        String token = tokenOf(user);
        flushAndClear();
        doThrow(new RestClientException("kakao unavailable")).when(kakaoUnlinkClient).unlink("k-1");

        deleteMe(token).andExpect(status().isNoContent());
        flushAndClear();

        verify(kakaoUnlinkClient, times(1)).unlink("k-1");
        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(data.countExisting()).isZero();
    }

    @Test
    void deleteWithoutToken_is401() throws Exception {
        User user = TestUsers.learner(userRepository, "delete-no-token");
        flushAndClear();

        mockMvc.perform(delete("/api/users/me")).andExpect(status().isUnauthorized());

        assertThat(userRepository.existsById(user.getId())).isTrue();
        verifyNoInteractions(kakaoUnlinkClient);
    }

    // ---- 도우미 ----

    private ResultActions deleteMe(String token) throws Exception {
        return mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + token));
    }

    private String tokenOf(User user) {
        return jwtTokenProvider.generateToken(user.getId(), user.getRole());
    }

    private User kakaoUser() {
        return userRepository.save(new User(
                OAuthProvider.KAKAO, "k-1", "Kakao Learner", "kakao-delete@example.com", LocalDateTime.now()
        ));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private long sessionCountOf(Long userId) {
        return entityManager.createQuery(
                        "SELECT COUNT(s) FROM SpeakingSession s WHERE s.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    /**
     * 사용자 하나에 딸린 학습 데이터 전부를 만든다.
     * 일반 회화: 세션·설정·메시지 2개·피드백·교정(메시지 참조).
     * 면접: 세션·설정·첫 질문·답변·답변 평가·항목 점수·꼬리 질문(부모 질문과 원본 답변 참조)·꼬리 질문 답변·면접 피드백.
     * 꼬리 질문의 source_answer_id와 답변의 question_id가 서로 참조하는 고리를 일부러 만든다.
     */
    private LearningData createLearningData(User user) {
        LocalDateTime now = LocalDateTime.now();
        LearningData data = new LearningData();

        SpeakingSession conversation = speakingSessionRepository.save(
                new SpeakingSession(user, SessionType.CONVERSATION, SessionStatus.COMPLETED, now, now));
        data.sessionIds.add(conversation.getId());
        data.conversationSettingIds.add(conversationSettingRepository.save(new ConversationSetting(
                conversation, "CAFE", "점원", "친절함", "테스트 상황",
                Difficulty.BEGINNER, SubtitleMode.JAPANESE, now)).getId());
        SpeakingMessage aiMessage = speakingMessageRepository.save(
                new SpeakingMessage(conversation, 1, Speaker.AI, "CONVERSATION", "いらっしゃいませ", now));
        SpeakingMessage userMessage = speakingMessageRepository.save(
                new SpeakingMessage(conversation, 2, Speaker.USER, "CONVERSATION", "コーヒーをください", now));
        data.messageIds.add(aiMessage.getId());
        data.messageIds.add(userMessage.getId());
        ConversationFeedback conversationFeedback = conversationFeedbackRepository.save(new ConversationFeedback(
                conversation, com.aijapanese.speaking.conversation.entity.GenerationStatus.COMPLETED,
                "요약", "자연스러움", "문법", "어휘", List.of("강점"), "다음 팁", now));
        data.conversationFeedbackIds.add(conversationFeedback.getId());
        data.correctionIds.add(conversationCorrectionRepository.save(new ConversationCorrection(
                conversationFeedback, userMessage, CorrectionCategory.NATURALNESS,
                "コーヒーをください", "コーヒーをお願いします", "こーひーをおねがいします",
                "커피 부탁합니다", "설명", 1)).getId());

        SpeakingSession interview = speakingSessionRepository.save(
                new SpeakingSession(user, SessionType.INTERVIEW, SessionStatus.COMPLETED, now, now));
        data.sessionIds.add(interview.getId());
        data.interviewSettingIds.add(interviewSettingRepository.save(new InterviewSetting(
                interview, "백엔드 개발자", "GENERAL", Difficulty.BEGINNER, null,
                SubtitleMode.JAPANESE, "JP", "ja", "ko", now)).getId());
        data.messageIds.add(speakingMessageRepository.save(
                new SpeakingMessage(interview, 1, Speaker.AI, "QUESTION", "自己紹介をお願いします。", now)).getId());

        InterviewQuestion initial = interviewQuestionRepository.save(new InterviewQuestion(
                interview, null, null, QuestionKind.INITIAL, "自己紹介をお願いします。", 1, QuestionStatus.ANSWERED, now));
        InterviewAnswer initialAnswer = interviewAnswerRepository.save(
                new InterviewAnswer(initial, "よろしくお願いします。", now));
        InterviewAnswerFeedback answerFeedback = interviewAnswerFeedbackRepository.save(new InterviewAnswerFeedback(
                initialAnswer, new BigDecimal("3.50"), "평가 요약", List.of("강점"), List.of("약점"), "v1",
                com.aijapanese.speaking.interview.entity.GenerationStatus.COMPLETED, now));
        InterviewEvaluationScore score = interviewEvaluationScoreRepository.save(new InterviewEvaluationScore(
                answerFeedback, EvaluationCriterion.COMMUNICATION, new BigDecimal("3.00"), "좋음", true));
        InterviewQuestion followUp = interviewQuestionRepository.save(new InterviewQuestion(
                interview, initial, initialAnswer, QuestionKind.FOLLOW_UP, "もう少し詳しく。", 2, QuestionStatus.ANSWERED, now));
        InterviewAnswer followUpAnswer = interviewAnswerRepository.save(
                new InterviewAnswer(followUp, "はい。", now));
        data.questionIds.add(initial.getId());
        data.questionIds.add(followUp.getId());
        data.answerIds.add(initialAnswer.getId());
        data.answerIds.add(followUpAnswer.getId());
        data.answerFeedbackIds.add(answerFeedback.getId());
        data.scoreIds.add(score.getId());
        data.interviewFeedbackIds.add(interviewFeedbackRepository.save(new InterviewFeedback(
                interview, new BigDecimal("3.50"), "총평", List.of("강점"), List.of("개선점"), 2, "v1",
                com.aijapanese.speaking.interview.entity.GenerationStatus.COMPLETED, now)).getId());

        return data;
    }

    /** 만든 행의 id 묶음. 저장소로 지금 남아 있는 행 수를 센다. */
    private class LearningData {
        final List<Long> sessionIds = new ArrayList<>();
        final List<Long> messageIds = new ArrayList<>();
        final List<Long> conversationSettingIds = new ArrayList<>();
        final List<Long> conversationFeedbackIds = new ArrayList<>();
        final List<Long> correctionIds = new ArrayList<>();
        final List<Long> interviewSettingIds = new ArrayList<>();
        final List<Long> questionIds = new ArrayList<>();
        final List<Long> answerIds = new ArrayList<>();
        final List<Long> answerFeedbackIds = new ArrayList<>();
        final List<Long> scoreIds = new ArrayList<>();
        final List<Long> interviewFeedbackIds = new ArrayList<>();

        int size() {
            return sessionIds.size() + messageIds.size() + conversationSettingIds.size()
                    + conversationFeedbackIds.size() + correctionIds.size() + interviewSettingIds.size()
                    + questionIds.size() + answerIds.size() + answerFeedbackIds.size() + scoreIds.size()
                    + interviewFeedbackIds.size();
        }

        int countExisting() {
            return found(speakingSessionRepository, sessionIds)
                    + found(speakingMessageRepository, messageIds)
                    + found(conversationSettingRepository, conversationSettingIds)
                    + found(conversationFeedbackRepository, conversationFeedbackIds)
                    + found(conversationCorrectionRepository, correctionIds)
                    + found(interviewSettingRepository, interviewSettingIds)
                    + found(interviewQuestionRepository, questionIds)
                    + found(interviewAnswerRepository, answerIds)
                    + found(interviewAnswerFeedbackRepository, answerFeedbackIds)
                    + found(interviewEvaluationScoreRepository, scoreIds)
                    + found(interviewFeedbackRepository, interviewFeedbackIds);
        }

        private <T> int found(JpaRepository<T, Long> repository, List<Long> ids) {
            return repository.findAllById(ids).size();
        }
    }
}
