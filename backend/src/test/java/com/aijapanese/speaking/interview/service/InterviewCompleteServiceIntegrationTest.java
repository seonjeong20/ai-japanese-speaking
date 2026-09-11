package com.aijapanese.speaking.interview.service;

import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.interview.ai.InterviewAiService;
import com.aijapanese.speaking.interview.ai.InterviewAnswerCoachingAiResult;
import com.aijapanese.speaking.interview.ai.InterviewAnswerCoachingAiService;
import com.aijapanese.speaking.interview.ai.InterviewAnswerEvaluationAiResult;
import com.aijapanese.speaking.interview.ai.InterviewAnswerEvaluationAiService;
import com.aijapanese.speaking.interview.ai.InterviewFinalFeedbackAiResult;
import com.aijapanese.speaking.interview.ai.InterviewFinalFeedbackAiService;
import com.aijapanese.speaking.interview.ai.InterviewFirstQuestionResult;
import com.aijapanese.speaking.interview.dto.InterviewFeedbackResponse;
import com.aijapanese.speaking.interview.dto.InterviewStartRequest;
import com.aijapanese.speaking.interview.entity.EvaluationCriterion;
import com.aijapanese.speaking.interview.entity.GenerationStatus;
import com.aijapanese.speaking.interview.exception.InterviewAnswerConflictException;
import com.aijapanese.speaking.interview.exception.InterviewFeedbackNotFoundException;
import com.aijapanese.speaking.interview.repository.InterviewFeedbackRepository;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Interview Complete + Final Feedback(Phase 4) 통합 테스트.
 * 모든 AI Service는 @MockitoBean으로 대체한다 — 실제 OpenAI API를 절대 호출하지 않는다.
 */
@SpringBootTest
@Transactional
class InterviewCompleteServiceIntegrationTest {

    @Autowired private InterviewService interviewService;
    @Autowired private SpeakingSessionRepository speakingSessionRepository;
    @Autowired private InterviewFeedbackRepository interviewFeedbackRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private InterviewAiService interviewAiService;
    @MockitoBean private InterviewAnswerEvaluationAiService evaluationAiService;
    @MockitoBean private InterviewAnswerCoachingAiService coachingAiService;
    @MockitoBean private InterviewFinalFeedbackAiService finalFeedbackAiService;

    @Test
    void completeInterview_aggregatesScoreAndSavesFinalFeedback() {
        StartedInterview started = driveToCompletion("complete-success@example.com");

        var response = interviewService.completeInterview(started.sessionId(), started.userId());

        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(response.endedAt()).isNotNull();

        var session = speakingSessionRepository.findById(started.sessionId()).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);

        var feedback = interviewFeedbackRepository.findBySession_Id(started.sessionId()).orElseThrow();
        // 5개 질문 모두 evaluation(80,70,60,90,75, STAR 미적용)으로 응답 -> 5개 criterion 평균은 각 답변 동일값이므로
        // 전체 평균도 (80+70+60+90+75)/5 = 75.00
        assertThat(feedback.getOverallScore()).isEqualByComparingTo("75.00");
        assertThat(feedback.getEvaluatedAnswerCount()).isEqualTo(5);
        assertThat(feedback.getGenerationStatus()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(feedback.getSummary()).isEqualTo("전체적으로 안정적인 면접이었습니다.");
    }

    @Test
    void completeInterview_rejectsWhenUnansweredQuestionRemains() {
        Long userId = createUser("complete-unanswered@example.com", UserRole.LEARNER);
        stubFirstQuestion();
        var start = interviewService.startInterview(userId, sampleRequest());

        assertThatThrownBy(() -> interviewService.completeInterview(start.sessionId(), userId))
                .isInstanceOf(InterviewAnswerConflictException.class);

        var session = speakingSessionRepository.findById(start.sessionId()).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    /**
     * completeInterview 전체가 하나의 @Transactional이므로, 실제 rollback 여부는 이 테스트
     * 자신의 (참여) 트랜잭션 안에서는 관찰할 수 없다(자신의 미커밋 write가 항상 보임 — 다른
     * NOT_SUPPORTED 테스트들과 동일한 제약). 여기서는 예외가 세션/Feedback 저장 전에
     * 전파되어 호출자가 성공 응답을 받지 못함을 검증한다: 실제 rollback 여부는 Spring
     * @Transactional의 기본 보장(RuntimeException 시 rollback-only)에 의존한다.
     */
    @Test
    void completeInterview_propagatesExceptionWhenFinalFeedbackAiFails() {
        StartedInterview started = driveToCompletion("complete-ai-fail@example.com");
        org.mockito.Mockito.doThrow(new AiServiceException("final feedback failed"))
                .when(finalFeedbackAiService).summarize(any(), any(), any(), any());

        assertThatThrownBy(() -> interviewService.completeInterview(started.sessionId(), started.userId()))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void getFeedback_readsFromDbWithoutCallingAiAgain() {
        StartedInterview started = driveToCompletion("feedback-db-only@example.com");
        interviewService.completeInterview(started.sessionId(), started.userId());

        InterviewFeedbackResponse response = interviewService.getFeedback(started.sessionId(), started.userId());

        assertThat(response.sessionId()).isEqualTo(started.sessionId());
        assertThat(response.overall().overallScore()).isEqualByComparingTo("75.00");
        assertThat(response.answers()).hasSize(5);
        assertThat(response.answers().get(0).scores()).hasSize(6);
        // 요약을 생성한 뒤 getFeedback을 한 번 더 호출해도 종합 Feedback AI가 재호출되지 않아야 한다.
        verify(finalFeedbackAiService, org.mockito.Mockito.times(1)).summarize(any(), any(), any(), any());
    }

    @Test
    void getFeedback_throwsWhenNotYetCompleted() {
        Long userId = createUser("feedback-not-found@example.com", UserRole.LEARNER);
        stubFirstQuestion();
        var start = interviewService.startInterview(userId, sampleRequest());

        assertThatThrownBy(() -> interviewService.getFeedback(start.sessionId(), userId))
                .isInstanceOf(InterviewFeedbackNotFoundException.class);
    }

    @Test
    void otherUserCannotCompleteOrReadFeedback() {
        Long ownerId = createUser("complete-owner@example.com", UserRole.LEARNER);
        Long otherId = createUser("complete-other@example.com", UserRole.LEARNER);
        stubFirstQuestion();
        var start = interviewService.startInterview(ownerId, sampleRequest());

        assertThatThrownBy(() -> interviewService.completeInterview(start.sessionId(), otherId))
                .isInstanceOf(com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException.class);
        assertThatThrownBy(() -> interviewService.getFeedback(start.sessionId(), otherId))
                .isInstanceOf(com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException.class);
    }

    private void stubFirstQuestion() {
        when(interviewAiService.generateFirstQuestion(any()))
                .thenReturn(new InterviewFirstQuestionResult("自己紹介をお願いします。"));
        when(interviewAiService.generateNextInitialQuestion(any(), any()))
                .thenReturn(new InterviewFirstQuestionResult("志望動機を教えてください。"));
        doReturn(coaching()).when(coachingAiService).coach(any(), any(), any(), any(), any(Boolean.class));
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation());
        doReturn(finalFeedback()).when(finalFeedbackAiService).summarize(any(), any(), any(), any());
    }

    /** 꼬리질문 없이(followUpNeeded=false) INITIAL 5개를 모두 답해 진행 가능한 질문이 없는 상태로 만든다. */
    private StartedInterview driveToCompletion(String email) {
        Long userId = createUser(email, UserRole.LEARNER);
        stubFirstQuestion();
        var start = interviewService.startInterview(userId, sampleRequest());

        Long currentQuestionId = start.firstQuestion().questionId();
        for (int i = 0; i < 5; i++) {
            var response = interviewService.submitTextAnswer(start.sessionId(), currentQuestionId, userId, "回答 " + i);
            if (!response.isComplete()) {
                currentQuestionId = response.nextQuestion().questionId();
            }
        }
        return new StartedInterview(userId, start.sessionId());
    }

    private Long createUser(String email, UserRole role) {
        Organization organization = organizationRepository.save(
                new Organization("Test Org " + email, OrganizationStatus.ACTIVE, LocalDateTime.now())
        );
        return userRepository.save(new User(
                organization, "Test " + role, email, passwordEncoder.encode("password123"),
                null, role, UserStatus.ACTIVE, LocalDateTime.now()
        )).getId();
    }

    private InterviewStartRequest sampleRequest() {
        return new InterviewStartRequest(
                "Backend Developer", "BEHAVIORAL", Difficulty.INTERMEDIATE, null, SubtitleMode.JAPANESE
        );
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

    private record StartedInterview(Long userId, Long sessionId) {
    }
}
