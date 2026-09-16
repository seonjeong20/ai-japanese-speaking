package com.aijapanese.speaking.interview.service;

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
import com.aijapanese.speaking.interview.dto.InterviewStartRequest;
import com.aijapanese.speaking.interview.entity.EvaluationCriterion;
import com.aijapanese.speaking.interview.exception.InterviewFeedbackNotFoundException;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotFoundException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotInProgressException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Interview 중도 종료(abort, Phase 8) 통합 테스트.
 * ConversationServiceIntegrationTest의 abortConversation 테스트와 동일한 관점을 검증한다:
 * 본인 IN_PROGRESS 세션만 ABORTED로 전환 가능하고, 종합 Feedback AI는 절대 호출되지 않는다.
 */
@SpringBootTest
@Transactional
class InterviewAbortServiceIntegrationTest {

    @Autowired private InterviewService interviewService;
    @Autowired private SpeakingSessionRepository speakingSessionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private InterviewAiService interviewAiService;
    @MockitoBean private InterviewAnswerEvaluationAiService evaluationAiService;
    @MockitoBean private InterviewAnswerCoachingAiService coachingAiService;
    @MockitoBean private InterviewFinalFeedbackAiService finalFeedbackAiService;

    @Test
    void abortInterview_ownInProgressSession_succeeds() {
        Long userId = createUser("interview-abort-1@example.com");
        stubFirstQuestion();
        var start = interviewService.startInterview(userId, sampleRequest());

        var aborted = interviewService.abortInterview(start.sessionId(), userId);

        assertThat(aborted.status()).isEqualTo(SessionStatus.ABORTED);
        assertThat(aborted.endedAt()).isNotNull();
        assertThat(aborted.durationSeconds()).isGreaterThanOrEqualTo(0);
        assertThat(aborted.feedbackGenerationStatus()).isNull();

        var session = speakingSessionRepository.findById(start.sessionId()).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ABORTED);
    }

    @Test
    void abortInterview_doesNotGenerateFinalFeedback() {
        Long userId = createUser("interview-abort-2@example.com");
        stubFirstQuestion();
        var start = interviewService.startInterview(userId, sampleRequest());

        interviewService.abortInterview(start.sessionId(), userId);

        verify(finalFeedbackAiService, never()).summarize(any(), any(), any(), any());
        assertThatThrownBy(() -> interviewService.getFeedback(start.sessionId(), userId))
                .isInstanceOf(InterviewFeedbackNotFoundException.class);
    }

    @Test
    void abortInterview_rejectsOtherUsersSession() {
        Long ownerId = createUser("interview-abort-3-owner@example.com");
        Long otherId = createUser("interview-abort-3-other@example.com");
        stubFirstQuestion();
        var start = interviewService.startInterview(ownerId, sampleRequest());

        assertThatThrownBy(() -> interviewService.abortInterview(start.sessionId(), otherId))
                .isInstanceOf(SpeakingSessionAccessDeniedException.class);
    }

    @Test
    void abortInterview_throwsWhenSessionNotFound() {
        Long userId = createUser("interview-abort-4@example.com");

        assertThatThrownBy(() -> interviewService.abortInterview(999_999_999L, userId))
                .isInstanceOf(SpeakingSessionNotFoundException.class);
    }

    @Test
    void abortInterview_rejectsAlreadyCompletedSession() {
        Long userId = createUser("interview-abort-5@example.com");
        stubFirstQuestion();
        var start = interviewService.startInterview(userId, sampleRequest());

        Long currentQuestionId = start.firstQuestion().questionId();
        for (int i = 0; i < 5; i++) {
            var response = interviewService.submitTextAnswer(start.sessionId(), currentQuestionId, userId, "回答 " + i);
            if (!response.isComplete()) {
                currentQuestionId = response.nextQuestion().questionId();
            }
        }
        interviewService.completeInterview(start.sessionId(), userId);

        assertThatThrownBy(() -> interviewService.abortInterview(start.sessionId(), userId))
                .isInstanceOf(SpeakingSessionNotInProgressException.class);
    }

    @Test
    void abortInterview_rejectsAlreadyAbortedSession() {
        Long userId = createUser("interview-abort-6@example.com");
        stubFirstQuestion();
        var start = interviewService.startInterview(userId, sampleRequest());
        interviewService.abortInterview(start.sessionId(), userId);

        assertThatThrownBy(() -> interviewService.abortInterview(start.sessionId(), userId))
                .isInstanceOf(SpeakingSessionNotInProgressException.class);
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

    private Long createUser(String email) {
        Organization organization = organizationRepository.save(
                new Organization("Test Org " + email, OrganizationStatus.ACTIVE, LocalDateTime.now())
        );
        return userRepository.save(new User(
                organization, "Test Learner", email, passwordEncoder.encode("password123"),
                null, UserRole.LEARNER, UserStatus.ACTIVE, LocalDateTime.now()
        )).getId();
    }

    private InterviewStartRequest sampleRequest() {
        return new InterviewStartRequest(
                "Backend Developer", "BEHAVIORAL", Difficulty.INTERMEDIATE, null, SubtitleMode.JAPANESE
        );
    }
}
