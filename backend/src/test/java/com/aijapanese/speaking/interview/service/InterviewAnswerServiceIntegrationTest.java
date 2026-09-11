package com.aijapanese.speaking.interview.service;

import com.aijapanese.speaking.ai.AiClient;
import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.interview.ai.InterviewAiService;
import com.aijapanese.speaking.interview.ai.InterviewAnswerCoachingAiResult;
import com.aijapanese.speaking.interview.ai.InterviewAnswerCoachingAiService;
import com.aijapanese.speaking.interview.ai.InterviewAnswerEvaluationAiResult;
import com.aijapanese.speaking.interview.ai.InterviewAnswerEvaluationAiService;
import com.aijapanese.speaking.interview.ai.InterviewFirstQuestionResult;
import com.aijapanese.speaking.interview.dto.InterviewStartRequest;
import com.aijapanese.speaking.interview.entity.EvaluationCriterion;
import com.aijapanese.speaking.interview.entity.GenerationStatus;
import com.aijapanese.speaking.interview.entity.QuestionKind;
import com.aijapanese.speaking.interview.entity.QuestionStatus;
import com.aijapanese.speaking.interview.exception.InterviewAnswerConflictException;
import com.aijapanese.speaking.interview.exception.InterviewQuestionNotFoundException;
import com.aijapanese.speaking.interview.repository.InterviewAnswerFeedbackRepository;
import com.aijapanese.speaking.interview.repository.InterviewAnswerRepository;
import com.aijapanese.speaking.interview.repository.InterviewEvaluationScoreRepository;
import com.aijapanese.speaking.interview.repository.InterviewQuestionRepository;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.Speaker;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotFoundException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotInProgressException;
import com.aijapanese.speaking.speaking.repository.SpeakingMessageRepository;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterviewAnswerServiceIntegrationTest {

    @Autowired private InterviewService interviewService;
    @Autowired private SpeakingSessionRepository speakingSessionRepository;
    @Autowired private SpeakingMessageRepository speakingMessageRepository;
    @Autowired private InterviewQuestionRepository questionRepository;
    @Autowired private InterviewAnswerRepository answerRepository;
    @Autowired private InterviewAnswerFeedbackRepository feedbackRepository;
    @Autowired private InterviewEvaluationScoreRepository scoreRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private MockMvc mockMvc;

    @MockitoBean private InterviewAiService interviewAiService;
    @MockitoBean private InterviewAnswerEvaluationAiService evaluationAiService;
    @MockitoBean private InterviewAnswerCoachingAiService coachingAiService;
    @MockitoBean private AiClient aiClient;

    @BeforeEach
    void stubFirstQuestion() {
        when(interviewAiService.generateFirstQuestion(any()))
                .thenReturn(new InterviewFirstQuestionResult("困難な課題を解決した経験を教えてください。"));
        when(interviewAiService.generateNextInitialQuestion(any(), any()))
                .thenReturn(new InterviewFirstQuestionResult("志望動機を教えてください。"));
        doReturn(coaching(false)).when(coachingAiService)
                .coach(any(), any(), any(), any(), any(Boolean.class));
    }

    @Test
    void learnerAudioAnswer_savesTranscriptAnalysisFeedbackAndStarScore() {
        StartedInterview started = startInterview("answer-success@example.com", UserRole.LEARNER);
        when(aiClient.transcribe(any())).thenReturn("チームで原因を分析し、問題を解決しました。");
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation(true));

        var response = interviewService.submitAudioAnswer(
                started.sessionId(), started.questionId(), started.userId(), new ByteArrayResource(new byte[]{1, 2})
        );

        assertThat(response.answerText()).isEqualTo("チームで原因を分析し、問題を解決しました。");
        assertThat(response.feedback().overallScore()).isEqualByComparingTo("76.67");
        assertThat(response.feedback().generationStatus()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(response.nextQuestion().questionKind()).isEqualTo(QuestionKind.INITIAL);
        assertThat(response.isComplete()).isFalse();

        var answer = answerRepository.findByQuestion_Id(started.questionId()).orElseThrow();
        assertThat(answer.getAnswerText()).isEqualTo(response.answerText());

        var messages = speakingMessageRepository.findBySession_IdOrderBySequenceNoAsc(started.sessionId());
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getSpeaker()).isEqualTo(Speaker.AI);
        assertThat(messages.get(1).getSpeaker()).isEqualTo(Speaker.USER);
        assertThat(messages.get(1).getMessageType()).isEqualTo("ANSWER");
        assertThat(messages.get(1).getContent()).isEqualTo(response.answerText());
        assertThat(messages.get(2).getSpeaker()).isEqualTo(Speaker.AI);
        assertThat(messages.get(2).getMessageType()).isEqualTo("QUESTION");

        var question = questionRepository.findById(started.questionId()).orElseThrow();
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.ANSWERED);
        assertThat(question.getIntent()).isEqualTo("문제 해결 경험 확인");
        assertThat(question.getQuestionType()).isEqualTo("BEHAVIORAL");
        assertThat(question.getCoreCompetencies()).containsExactly("문제 해결", "협업");
        assertThat(question.isStarRecommended()).isTrue();

        var feedback = feedbackRepository.findByAnswer_Id(answer.getId()).orElseThrow();
        assertThat(feedback.getEvaluationSummary()).isEqualTo("질문에 맞는 경험을 간결하게 설명했습니다.");
        assertThat(feedback.getStrengths()).containsExactly("질문에 직접 답함");
        assertThat(feedback.getWeaknesses()).containsExactly("성과가 구체적이지 않음");
        assertThat(feedback.getCoachingSummary()).isEqualTo("핵심 행동과 결과를 더 구체화하세요.");
        assertThat(feedback.getImprovementTips()).containsExactly("본인의 역할을 먼저 밝히세요.");
        assertThat(feedback.getImprovedAnswer()).isEqualTo("私は[担当した役割]として問題を解決しました。");

        var scores = scoreRepository.findByAnswerFeedback_IdOrderByIdAsc(feedback.getId());
        assertThat(scores).hasSize(6);
        assertThat(scores).anySatisfy(score -> {
            assertThat(score.getCriterion()).isEqualTo(EvaluationCriterion.STAR_STRUCTURE);
            assertThat(score.isApplicable()).isTrue();
            assertThat(score.getScore()).isEqualByComparingTo("85");
        });
    }

    @Test
    void starNotRecommended_savesNullNonApplicableAndExcludesItFromAverage() {
        StartedInterview started = startInterview("star-na@example.com", UserRole.LEARNER);
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation(false));

        var response = interviewService.submitTextAnswer(
                started.sessionId(), started.questionId(), started.userId(), "志望動機を説明します。"
        );

        assertThat(response.feedback().overallScore()).isEqualByComparingTo("75.00");
        var feedback = feedbackRepository.findByAnswer_Id(response.answerId()).orElseThrow();
        var star = scoreRepository.findByAnswerFeedback_IdOrderByIdAsc(feedback.getId()).stream()
                .filter(score -> score.getCriterion() == EvaluationCriterion.STAR_STRUCTURE)
                .findFirst().orElseThrow();
        assertThat(star.isApplicable()).isFalse();
        assertThat(star.getScore()).isNull();
    }

    @Test
    void otherUserCannotAnswerAndMissingSessionOrQuestionReturnsDomainErrors() {
        StartedInterview owner = startInterview("owner@example.com", UserRole.LEARNER);
        Long otherUserId = createUser("other@example.com", UserRole.LEARNER);

        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                owner.sessionId(), owner.questionId(), otherUserId, "回答"
        )).isInstanceOf(SpeakingSessionAccessDeniedException.class);
        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                Long.MAX_VALUE, owner.questionId(), owner.userId(), "回答"
        )).isInstanceOf(SpeakingSessionNotFoundException.class);
        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                owner.sessionId(), Long.MAX_VALUE, owner.userId(), "回答"
        )).isInstanceOf(InterviewQuestionNotFoundException.class);
    }

    @Test
    void completedAndAbortedSessionsRejectAnswers() {
        StartedInterview completed = startInterview("completed@example.com", UserRole.LEARNER);
        speakingSessionRepository.findById(completed.sessionId()).orElseThrow().complete(LocalDateTime.now());
        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                completed.sessionId(), completed.questionId(), completed.userId(), "回答"
        )).isInstanceOf(SpeakingSessionNotInProgressException.class);

        StartedInterview aborted = startInterview("aborted@example.com", UserRole.LEARNER);
        speakingSessionRepository.findById(aborted.sessionId()).orElseThrow().abort(LocalDateTime.now());
        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                aborted.sessionId(), aborted.questionId(), aborted.userId(), "回答"
        )).isInstanceOf(SpeakingSessionNotInProgressException.class);
    }

    @Test
    void duplicateQuestionAnswerIsRejected() {
        StartedInterview started = startInterview("duplicate@example.com", UserRole.LEARNER);
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation(false));
        interviewService.submitTextAnswer(started.sessionId(), started.questionId(), started.userId(), "最初の回答");
        long questionCountAfterFirstRequest = questionRepository.count();

        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                started.sessionId(), started.questionId(), started.userId(), "二回目の回答"
        )).isInstanceOf(InterviewAnswerConflictException.class);
        assertThat(answerRepository.count()).isEqualTo(1);
        assertThat(questionRepository.count()).isEqualTo(questionCountAfterFirstRequest);
    }

    @Test
    void initialAnswerCanCreateFollowUpWithDbmlRelations() {
        StartedInterview started = startInterview("initial-follow-up@example.com", UserRole.LEARNER);
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation(true));
        when(coachingAiService.coach(any(), any(), any(), any(), any(Boolean.class)))
                .thenReturn(coaching(true));

        var response = interviewService.submitTextAnswer(
                started.sessionId(), started.questionId(), started.userId(), "問題を解決しました。"
        );

        assertThat(response.isComplete()).isFalse();
        assertThat(response.nextQuestion().questionKind()).isEqualTo(QuestionKind.FOLLOW_UP);
        var followUp = questionRepository.findById(response.nextQuestion().questionId()).orElseThrow();
        assertThat(followUp.getParentQuestion().getId()).isEqualTo(started.questionId());
        assertThat(followUp.getSourceAnswerId()).isEqualTo(response.answerId());
        assertThat(followUp.getSequenceNo()).isEqualTo(2);
        assertThat(speakingMessageRepository.findBySession_IdOrderBySequenceNoAsc(started.sessionId()))
                .extracting(message -> message.getSpeaker())
                .containsExactly(Speaker.AI, Speaker.USER, Speaker.AI);
    }

    @Test
    void followUpAnswerCannotCreateNestedFollowUpAndMovesToNextInitial() {
        StartedInterview started = startInterview("no-nested-follow-up@example.com", UserRole.LEARNER);
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation(true));
        when(coachingAiService.coach(any(), any(), any(), any(), any(Boolean.class)))
                .thenReturn(coaching(true));

        var firstResponse = interviewService.submitTextAnswer(
                started.sessionId(), started.questionId(), started.userId(), "最初の回答"
        );
        var secondResponse = interviewService.submitTextAnswer(
                started.sessionId(), firstResponse.nextQuestion().questionId(), started.userId(), "追加の回答"
        );

        assertThat(firstResponse.nextQuestion().questionKind()).isEqualTo(QuestionKind.FOLLOW_UP);
        assertThat(secondResponse.nextQuestion().questionKind()).isEqualTo(QuestionKind.INITIAL);
        var nextInitial = questionRepository.findById(secondResponse.nextQuestion().questionId()).orElseThrow();
        assertThat(nextInitial.getParentQuestion()).isNull();
        assertThat(nextInitial.getSourceAnswer()).isNull();
    }

    @Test
    void backendEnforcesFiveInitialsOneFollowUpEachAndTenQuestionMaximum() {
        StartedInterview started = startInterview("maximum-progression@example.com", UserRole.LEARNER);
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation(true));
        when(coachingAiService.coach(any(), any(), any(), any(), any(Boolean.class)))
                .thenAnswer(invocation -> coaching(invocation.getArgument(4)));

        Long currentQuestionId = started.questionId();
        int submittedAnswers = 0;
        while (true) {
            var response = interviewService.submitTextAnswer(
                    started.sessionId(), currentQuestionId, started.userId(), "回答 " + submittedAnswers
            );
            submittedAnswers++;
            if (response.isComplete()) {
                assertThat(response.nextQuestion()).isNull();
                break;
            }
            currentQuestionId = response.nextQuestion().questionId();
        }

        var questions = questionRepository.findBySession_IdOrderBySequenceNoAsc(started.sessionId());
        assertThat(submittedAnswers).isEqualTo(10);
        assertThat(questions).hasSize(10);
        assertThat(questions).filteredOn(question -> question.getQuestionKind() == QuestionKind.INITIAL).hasSize(5);
        assertThat(questions).filteredOn(question -> question.getQuestionKind() == QuestionKind.FOLLOW_UP).hasSize(5);
        assertThat(questions).extracting(question -> question.getSequenceNo())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(questions).filteredOn(question -> question.getQuestionKind() == QuestionKind.FOLLOW_UP)
                .allSatisfy(question -> {
                    assertThat(question.getParentQuestion()).isNotNull();
                    assertThat(question.getParentQuestion().getQuestionKind()).isEqualTo(QuestionKind.INITIAL);
                    assertThat(question.getSourceAnswer()).isNotNull();
                });
        verify(coachingAiService, times(10)).coach(any(), any(), any(), any(), any(Boolean.class));
        verify(interviewAiService, times(4)).generateNextInitialQuestion(any(), any());
    }

    @Test
    void coachingOrNextInitialAiFailureLeavesNoPartialProgressionData() {
        StartedInterview coachingFailure = startInterview("coaching-fail@example.com", UserRole.LEARNER);
        when(evaluationAiService.evaluate(any(), any(), any())).thenReturn(evaluation(false));
        when(coachingAiService.coach(any(), any(), any(), any(), any(Boolean.class)))
                .thenThrow(new AiServiceException("coaching failed"));

        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                coachingFailure.sessionId(), coachingFailure.questionId(), coachingFailure.userId(), "回答"
        )).isInstanceOf(AiServiceException.class);
        assertNoAnswerData(coachingFailure);

        StartedInterview nextInitialFailure = startInterview("next-initial-fail@example.com", UserRole.LEARNER);
        doReturn(coaching(false)).when(coachingAiService)
                .coach(any(), any(), any(), any(), any(Boolean.class));
        when(interviewAiService.generateNextInitialQuestion(any(), any()))
                .thenThrow(new AiServiceException("next initial failed"));

        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                nextInitialFailure.sessionId(), nextInitialFailure.questionId(), nextInitialFailure.userId(), "回答"
        )).isInstanceOf(AiServiceException.class);
        assertNoAnswerData(nextInitialFailure);
    }

    @Test
    void sttOrEvaluationFailureLeavesNoPartialAnswerData() {
        StartedInterview sttFailure = startInterview("stt-fail@example.com", UserRole.LEARNER);
        when(aiClient.transcribe(any())).thenThrow(new AiServiceException("STT failed"));

        assertThatThrownBy(() -> interviewService.submitAudioAnswer(
                sttFailure.sessionId(), sttFailure.questionId(), sttFailure.userId(), new ByteArrayResource(new byte[]{1})
        )).isInstanceOf(AiServiceException.class);
        assertNoAnswerData(sttFailure);

        StartedInterview evaluationFailure = startInterview("evaluation-fail@example.com", UserRole.LEARNER);
        when(evaluationAiService.evaluate(any(), any(), any())).thenThrow(new AiServiceException("evaluation failed"));
        assertThatThrownBy(() -> interviewService.submitTextAnswer(
                evaluationFailure.sessionId(), evaluationFailure.questionId(), evaluationFailure.userId(), "回答"
        )).isInstanceOf(AiServiceException.class);
        assertNoAnswerData(evaluationFailure);
    }

    @Test
    void managerAndAdminCannotAccessAudioAnswerApi() throws Exception {
        StartedInterview interview = startInterview("api-owner@example.com", UserRole.LEARNER);
        MockMultipartFile audio = new MockMultipartFile("audio", "answer.webm", "audio/webm", new byte[]{1});

        for (UserRole role : List.of(UserRole.MANAGER, UserRole.ADMIN)) {
            Long userId = createUser(role.name().toLowerCase() + "-answer-api@example.com", role);
            String token = jwtTokenProvider.generateToken(userId, role);
            mockMvc.perform(multipart("/api/interviews/{sessionId}/questions/{questionId}/answer/audio",
                            interview.sessionId(), interview.questionId())
                            .file(audio)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isForbidden());
        }
    }

    private void assertNoAnswerData(StartedInterview started) {
        assertThat(answerRepository.findByQuestion_Id(started.questionId())).isEmpty();
        assertThat(feedbackRepository.count()).isZero();
        assertThat(scoreRepository.count()).isZero();
        assertThat(speakingMessageRepository.findBySession_IdOrderBySequenceNoAsc(started.sessionId()))
                .extracting(message -> message.getSpeaker())
                .containsExactly(Speaker.AI);
        assertThat(questionRepository.findById(started.questionId()).orElseThrow().getStatus())
                .isEqualTo(QuestionStatus.ASKED);
    }

    private StartedInterview startInterview(String email, UserRole role) {
        Long userId = createUser(email, role);
        var response = interviewService.startInterview(userId, new InterviewStartRequest(
                "Backend Developer", "BEHAVIORAL", Difficulty.INTERMEDIATE, null, SubtitleMode.JAPANESE
        ));
        return new StartedInterview(userId, response.sessionId(), response.firstQuestion().questionId());
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

    private InterviewAnswerEvaluationAiResult evaluation(boolean starRecommended) {
        return new InterviewAnswerEvaluationAiResult(
                "문제 해결 경험 확인",
                "BEHAVIORAL",
                List.of("문제 해결", "협업"),
                starRecommended,
                "질문에 맞는 경험을 간결하게 설명했습니다.",
                List.of("질문에 직접 답함"),
                List.of("성과가 구체적이지 않음"),
                List.of(
                        score(EvaluationCriterion.QUESTION_RELEVANCE, "80", true),
                        score(EvaluationCriterion.LOGICAL_THINKING, "70", true),
                        score(EvaluationCriterion.SPECIFICITY, "60", true),
                        score(EvaluationCriterion.COMMUNICATION, "90", true),
                        score(EvaluationCriterion.BUSINESS_JAPANESE, "75", true),
                        score(EvaluationCriterion.STAR_STRUCTURE, starRecommended ? "85" : null, starRecommended)
                )
        );
    }

    private InterviewAnswerCoachingAiResult coaching(boolean followUpNeeded) {
        return new InterviewAnswerCoachingAiResult(
                "핵심 행동과 결과를 더 구체화하세요.",
                List.of("본인의 역할을 먼저 밝히세요."),
                "私は[担当した役割]として問題を解決しました。",
                followUpNeeded,
                followUpNeeded ? "구체적인 역할 확인이 필요합니다." : "추가 확인이 필요하지 않습니다.",
                followUpNeeded ? "その際の具体的な役割を教えてください。" : null
        );
    }

    private InterviewAnswerEvaluationAiResult.CriterionEvaluation score(
            EvaluationCriterion criterion,
            String value,
            boolean applicable
    ) {
        return new InterviewAnswerEvaluationAiResult.CriterionEvaluation(
                criterion, value == null ? null : new BigDecimal(value), "평가 코멘트", applicable
        );
    }

    private record StartedInterview(Long userId, Long sessionId, Long questionId) {
    }
}
