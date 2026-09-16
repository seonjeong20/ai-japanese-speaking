package com.aijapanese.speaking.history.service;

import com.aijapanese.speaking.conversation.ai.ConversationFeedbackAiResult;
import com.aijapanese.speaking.conversation.ai.ConversationFeedbackAiService;
import com.aijapanese.speaking.conversation.dto.ConversationStartRequest;
import com.aijapanese.speaking.conversation.entity.CorrectionCategory;
import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.conversation.service.ConversationService;
import com.aijapanese.speaking.history.dto.HistoryDetailResponse;
import com.aijapanese.speaking.history.dto.HistoryListResponse;
import com.aijapanese.speaking.interview.ai.InterviewAiService;
import com.aijapanese.speaking.interview.ai.InterviewAnswerCoachingAiResult;
import com.aijapanese.speaking.interview.ai.InterviewAnswerCoachingAiService;
import com.aijapanese.speaking.interview.ai.InterviewAnswerEvaluationAiResult;
import com.aijapanese.speaking.interview.ai.InterviewAnswerEvaluationAiService;
import com.aijapanese.speaking.interview.ai.InterviewFinalFeedbackAiResult;
import com.aijapanese.speaking.interview.ai.InterviewFinalFeedbackAiService;
import com.aijapanese.speaking.interview.ai.InterviewFirstQuestionResult;
import com.aijapanese.speaking.interview.dto.InterviewStartRequest;
import com.aijapanese.speaking.interview.entity.EvaluationCriterion;
import com.aijapanese.speaking.interview.service.InterviewService;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.speaking.dto.SpeakingSessionResponse;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotFoundException;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * History Phase 5 통합 테스트. AI 관련 서비스는 모두 @MockitoBean으로 대체하며
 * (실제 OpenAI API 호출 없음), HistoryService는 저장된 데이터만 읽는다.
 */
@SpringBootTest
@Transactional
class HistoryServiceIntegrationTest {

    @Autowired private HistoryService historyService;
    @Autowired private ConversationService conversationService;
    @Autowired private InterviewService interviewService;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;

    @MockitoBean private ConversationFeedbackAiService conversationFeedbackAiService;
    @MockitoBean private InterviewAiService interviewAiService;
    @MockitoBean private InterviewAnswerEvaluationAiService evaluationAiService;
    @MockitoBean private InterviewAnswerCoachingAiService coachingAiService;
    @MockitoBean private InterviewFinalFeedbackAiService finalFeedbackAiService;

    @Test
    void getHistory_returnsOnlyOwnCompletedSessions_mostRecentFirst() {
        Long userId = createLearner("history-list-1@example.com");
        Long otherUserId = createLearner("history-list-other@example.com");

        // 본인 소유의 완료된 회화 세션 1개
        stubConversationFeedback();
        SpeakingSessionResponse conversationSession = conversationService.startConversation(
                userId, new ConversationStartRequest("카페", null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );
        conversationService.recordUserMessage(conversationSession.sessionId(), userId, "こんにちは");
        conversationService.completeConversation(conversationSession.sessionId(), userId);

        // 본인 소유의 진행 중(IN_PROGRESS) 회화 세션 -> 목록에 나오면 안 됨
        conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );

        // 다른 사용자의 완료 세션 -> 목록에 나오면 안 됨
        stubConversationFeedback();
        SpeakingSessionResponse otherSession = conversationService.startConversation(
                otherUserId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );
        conversationService.completeConversation(otherSession.sessionId(), otherUserId);

        HistoryListResponse history = historyService.getHistory(userId);

        assertThat(history.items()).hasSize(1);
        assertThat(history.items().get(0).sessionId()).isEqualTo(conversationSession.sessionId());
        assertThat(history.items().get(0).sessionType()).isEqualTo(SessionType.CONVERSATION);
        assertThat(history.items().get(0).status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(history.items().get(0).title()).isEqualTo("카페");
        assertThat(history.items().get(0).overallScore()).isNull();
    }

    @Test
    void getHistory_ordersMultipleCompletedSessionsByStartedAtDesc() {
        Long userId = createLearner("history-list-order@example.com");
        stubConversationFeedback();

        SpeakingSessionResponse first = conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );
        conversationService.completeConversation(first.sessionId(), userId);

        SpeakingSessionResponse second = conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );
        conversationService.completeConversation(second.sessionId(), userId);

        HistoryListResponse history = historyService.getHistory(userId);

        assertThat(history.items()).hasSize(2);
        assertThat(history.items().get(0).sessionId()).isEqualTo(second.sessionId());
        assertThat(history.items().get(1).sessionId()).isEqualTo(first.sessionId());
    }

    @Test
    void getHistoryDetail_conversation_returnsTranscriptAndFeedback() {
        Long userId = createLearner("history-detail-conv@example.com");
        stubConversationFeedback();

        SpeakingSessionResponse session = conversationService.startConversation(
                userId, new ConversationStartRequest("카페", "친구", "밝음", "설명", Difficulty.BEGINNER, SubtitleMode.JAPANESE_KOREAN)
        );
        conversationService.recordUserMessage(session.sessionId(), userId, "こんにちは");
        conversationService.completeConversation(session.sessionId(), userId);

        HistoryDetailResponse detail = historyService.getHistoryDetail(session.sessionId(), userId);

        assertThat(detail.sessionId()).isEqualTo(session.sessionId());
        assertThat(detail.sessionType()).isEqualTo(SessionType.CONVERSATION);
        assertThat(detail.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(detail.settings().get("situation")).isEqualTo("카페");
        assertThat(detail.transcript()).hasSize(1);
        assertThat(detail.transcript().get(0).content()).isEqualTo("こんにちは");
        assertThat(detail.conversationFeedback()).isNotNull();
        assertThat(detail.conversationFeedback().summary()).isEqualTo("전반적으로 잘했어요.");
        assertThat(detail.interviewFeedback()).isNull();
    }

    @Test
    void getHistoryDetail_interview_returnsQuestionsAndFinalFeedback() {
        InterviewSessionFixture fixture = driveInterviewToCompletion("history-detail-interview@example.com");

        HistoryDetailResponse detail = historyService.getHistoryDetail(fixture.sessionId(), fixture.userId());

        assertThat(detail.sessionType()).isEqualTo(SessionType.INTERVIEW);
        assertThat(detail.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(detail.settings().get("jobRole")).isEqualTo("Backend Developer");
        assertThat(detail.interviewFeedback()).isNotNull();
        assertThat(detail.interviewFeedback().overall().overallScore()).isEqualByComparingTo("75.00");
        assertThat(detail.interviewFeedback().answers()).hasSize(5);
        assertThat(detail.conversationFeedback()).isNull();
    }

    @Test
    void getHistory_interviewItem_includesOverallScore() {
        InterviewSessionFixture fixture = driveInterviewToCompletion("history-list-interview-score@example.com");

        HistoryListResponse history = historyService.getHistory(fixture.userId());

        assertThat(history.items()).hasSize(1);
        assertThat(history.items().get(0).sessionType()).isEqualTo(SessionType.INTERVIEW);
        assertThat(history.items().get(0).overallScore()).isEqualByComparingTo("75.00");
        assertThat(history.items().get(0).title()).isEqualTo("Backend Developer");
    }

    @Test
    void getHistoryDetail_rejectsOtherUsersSession() {
        Long ownerId = createLearner("history-detail-owner@example.com");
        Long otherId = createLearner("history-detail-other@example.com");
        stubConversationFeedback();
        SpeakingSessionResponse session = conversationService.startConversation(
                ownerId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );
        conversationService.completeConversation(session.sessionId(), ownerId);

        assertThatThrownBy(() -> historyService.getHistoryDetail(session.sessionId(), otherId))
                .isInstanceOf(SpeakingSessionAccessDeniedException.class);
    }

    @Test
    void getHistoryDetail_throwsWhenSessionNotFound() {
        Long userId = createLearner("history-detail-not-found@example.com");

        assertThatThrownBy(() -> historyService.getHistoryDetail(999_999_999L, userId))
                .isInstanceOf(SpeakingSessionNotFoundException.class);
    }

    private void stubConversationFeedback() {
        when(conversationFeedbackAiService.generateFeedback(any(), any())).thenReturn(
                new ConversationFeedbackAiResult(
                        "전반적으로 잘했어요.", "자연스러웠습니다.", "문법도 정확했습니다.", "어휘도 다양했습니다.",
                        List.of("인사를 잘했어요."), "다음엔 존댓말도 연습해보세요.",
                        List.of(new ConversationFeedbackAiResult.Correction(
                                CorrectionCategory.NATURALNESS, "こんにちは", "こんにちは!",
                                "こんにちは!", "안녕하세요!", "느낌표를 붙이면 더 자연스러워요.", 1
                        ))
                )
        );
    }

    private InterviewSessionFixture driveInterviewToCompletion(String email) {
        Long userId = createLearner(email);

        when(interviewAiService.generateFirstQuestion(any()))
                .thenReturn(new InterviewFirstQuestionResult("自己紹介をお願いします。"));
        when(interviewAiService.generateNextInitialQuestion(any(), any()))
                .thenReturn(new InterviewFirstQuestionResult("志望動機を教えてください。"));
        doReturn(coaching()).when(coachingAiService).coach(any(), any(), any(), any(), any(Boolean.class));
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation());
        doReturn(finalFeedback()).when(finalFeedbackAiService).summarize(any(), any(), any(), any());

        var start = interviewService.startInterview(userId, new InterviewStartRequest(
                "Backend Developer", "BEHAVIORAL", Difficulty.INTERMEDIATE, null, SubtitleMode.JAPANESE
        ));

        Long currentQuestionId = start.firstQuestion().questionId();
        for (int i = 0; i < 5; i++) {
            var response = interviewService.submitTextAnswer(start.sessionId(), currentQuestionId, userId, "回答 " + i);
            if (!response.isComplete()) {
                currentQuestionId = response.nextQuestion().questionId();
            }
        }

        interviewService.completeInterview(start.sessionId(), userId);
        return new InterviewSessionFixture(userId, start.sessionId());
    }

    private InterviewAnswerEvaluationAiResult evaluation() {
        return new InterviewAnswerEvaluationAiResult(
                "경험 확인", "BEHAVIORAL", List.of("문제 해결"), false,
                "질문에 맞는 답변이었습니다.", List.of("명확함"), List.of("구체성 부족"),
                List.of(
                        score(EvaluationCriterion.QUESTION_RELEVANCE, "80"),
                        score(EvaluationCriterion.LOGICAL_THINKING, "70"),
                        score(EvaluationCriterion.SPECIFICITY, "60"),
                        score(EvaluationCriterion.COMMUNICATION, "90"),
                        score(EvaluationCriterion.BUSINESS_JAPANESE, "75"),
                        new InterviewAnswerEvaluationAiResult.CriterionEvaluation(
                                EvaluationCriterion.STAR_STRUCTURE, null, "해당 없음", false
                        )
                )
        );
    }

    private InterviewAnswerEvaluationAiResult.CriterionEvaluation score(EvaluationCriterion criterion, String value) {
        return new InterviewAnswerEvaluationAiResult.CriterionEvaluation(criterion, new BigDecimal(value), "코멘트", true);
    }

    private InterviewAnswerCoachingAiResult coaching() {
        return new InterviewAnswerCoachingAiResult(
                "핵심을 더 구체화하세요.", List.of("역할을 먼저 밝히세요."),
                "私は[担当した役割]として対応しました。", false, "추가 확인이 필요하지 않습니다.", null
        );
    }

    private InterviewFinalFeedbackAiResult finalFeedback() {
        return new InterviewFinalFeedbackAiResult(
                "전체적으로 안정적인 면접이었습니다.",
                List.of("질문 의도에 맞게 답변함"),
                List.of("구체적인 수치를 더 포함하세요.")
        );
    }

    private Long createLearner(String email) {
        Organization organization = organizationRepository.save(
                new Organization("Test Org " + email, OrganizationStatus.ACTIVE, LocalDateTime.now())
        );
        User user = new User(
                organization, "Test Learner", email, "hash",
                null, UserRole.LEARNER, UserStatus.ACTIVE, LocalDateTime.now()
        );
        return userRepository.save(user).getId();
    }

    private record InterviewSessionFixture(Long userId, Long sessionId) {
    }
}
