package com.aijapanese.speaking.interview.service;

import com.aijapanese.speaking.ai.AiClient;
import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.interview.ai.InterviewAnswerCoachingAiResult;
import com.aijapanese.speaking.interview.ai.InterviewAnswerCoachingAiService;
import com.aijapanese.speaking.interview.ai.InterviewAnswerEvaluationAiResult;
import com.aijapanese.speaking.interview.ai.InterviewAnswerEvaluationAiService;
import com.aijapanese.speaking.interview.ai.InterviewAiService;
import com.aijapanese.speaking.interview.ai.InterviewAnsweredQuestionSummary;
import com.aijapanese.speaking.interview.ai.InterviewFinalFeedbackAiResult;
import com.aijapanese.speaking.interview.ai.InterviewFinalFeedbackAiService;
import com.aijapanese.speaking.interview.ai.InterviewFirstQuestionResult;
import com.aijapanese.speaking.interview.dto.InterviewAnswerEvaluationResponse;
import com.aijapanese.speaking.interview.dto.InterviewAnswerFeedbackResponse;
import com.aijapanese.speaking.interview.dto.InterviewAnswerResponse;
import com.aijapanese.speaking.interview.dto.InterviewEvaluationScoreResponse;
import com.aijapanese.speaking.interview.dto.InterviewFeedbackResponse;
import com.aijapanese.speaking.interview.dto.InterviewOverallFeedbackResponse;
import com.aijapanese.speaking.interview.dto.InterviewQuestionResponse;
import com.aijapanese.speaking.interview.dto.InterviewStartRequest;
import com.aijapanese.speaking.interview.dto.InterviewStartResponse;
import com.aijapanese.speaking.interview.entity.EvaluationCriterion;
import com.aijapanese.speaking.interview.entity.GenerationStatus;
import com.aijapanese.speaking.interview.entity.InterviewAnswer;
import com.aijapanese.speaking.interview.entity.InterviewAnswerFeedback;
import com.aijapanese.speaking.interview.entity.InterviewEvaluationScore;
import com.aijapanese.speaking.interview.entity.InterviewFeedback;
import com.aijapanese.speaking.interview.entity.InterviewQuestion;
import com.aijapanese.speaking.interview.entity.InterviewSetting;
import com.aijapanese.speaking.interview.entity.QuestionKind;
import com.aijapanese.speaking.interview.entity.QuestionStatus;
import com.aijapanese.speaking.interview.exception.InterviewAnswerConflictException;
import com.aijapanese.speaking.interview.exception.InterviewFeedbackNotFoundException;
import com.aijapanese.speaking.interview.exception.InterviewQuestionNotFoundException;
import com.aijapanese.speaking.interview.repository.InterviewAnswerFeedbackRepository;
import com.aijapanese.speaking.interview.repository.InterviewAnswerRepository;
import com.aijapanese.speaking.interview.repository.InterviewEvaluationScoreRepository;
import com.aijapanese.speaking.interview.repository.InterviewFeedbackRepository;
import com.aijapanese.speaking.interview.repository.InterviewQuestionRepository;
import com.aijapanese.speaking.interview.repository.InterviewSettingRepository;
import com.aijapanese.speaking.conversation.dto.SessionCompletionResponse;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.Speaker;
import com.aijapanese.speaking.speaking.entity.SpeakingMessage;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotFoundException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotInProgressException;
import com.aijapanese.speaking.speaking.repository.SpeakingMessageRepository;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InterviewService {

    // 현재 서비스는 일본 취업 면접 중심이므로 서버가 고정 지정한다 (OpenAPI 계약).
    private static final String TARGET_COUNTRY = "JAPAN";
    private static final String TARGET_LANGUAGE = "JAPANESE";
    private static final String FEEDBACK_LANGUAGE = "KOREAN";
    private static final int FIRST_QUESTION_SEQUENCE_NO = 1;
    private static final String QUESTION_MESSAGE_TYPE = "QUESTION";
    private static final String ANSWER_MESSAGE_TYPE = "ANSWER";
    private static final String RUBRIC_VERSION = "v1";
    private static final int MAX_INITIAL_QUESTIONS = 5;
    private static final int MAX_TOTAL_QUESTIONS = 10;

    private final SpeakingSessionRepository speakingSessionRepository;
    private final InterviewSettingRepository interviewSettingRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewAnswerFeedbackRepository interviewAnswerFeedbackRepository;
    private final InterviewEvaluationScoreRepository interviewEvaluationScoreRepository;
    private final InterviewFeedbackRepository interviewFeedbackRepository;
    private final SpeakingMessageRepository speakingMessageRepository;
    private final UserRepository userRepository;
    private final InterviewAiService interviewAiService;
    private final InterviewAnswerEvaluationAiService answerEvaluationAiService;
    private final InterviewAnswerCoachingAiService answerCoachingAiService;
    private final InterviewFinalFeedbackAiService finalFeedbackAiService;
    private final AiClient aiClient;

    public InterviewService(
            SpeakingSessionRepository speakingSessionRepository,
            InterviewSettingRepository interviewSettingRepository,
            InterviewQuestionRepository interviewQuestionRepository,
            InterviewAnswerRepository interviewAnswerRepository,
            InterviewAnswerFeedbackRepository interviewAnswerFeedbackRepository,
            InterviewEvaluationScoreRepository interviewEvaluationScoreRepository,
            InterviewFeedbackRepository interviewFeedbackRepository,
            SpeakingMessageRepository speakingMessageRepository,
            UserRepository userRepository,
            InterviewAiService interviewAiService,
            InterviewAnswerEvaluationAiService answerEvaluationAiService,
            InterviewAnswerCoachingAiService answerCoachingAiService,
            InterviewFinalFeedbackAiService finalFeedbackAiService,
            AiClient aiClient
    ) {
        this.speakingSessionRepository = speakingSessionRepository;
        this.interviewSettingRepository = interviewSettingRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewAnswerRepository = interviewAnswerRepository;
        this.interviewAnswerFeedbackRepository = interviewAnswerFeedbackRepository;
        this.interviewEvaluationScoreRepository = interviewEvaluationScoreRepository;
        this.interviewFeedbackRepository = interviewFeedbackRepository;
        this.speakingMessageRepository = speakingMessageRepository;
        this.userRepository = userRepository;
        this.interviewAiService = interviewAiService;
        this.answerEvaluationAiService = answerEvaluationAiService;
        this.answerCoachingAiService = answerCoachingAiService;
        this.finalFeedbackAiService = finalFeedbackAiService;
        this.aiClient = aiClient;
    }

    /**
     * 하나의 Transaction 안에서 Session 생성 → Setting 저장 → 첫 질문 AI 생성 → 질문 저장까지 수행한다.
     * 첫 질문 AI 호출이 실패(AiServiceException)하면 이 메서드 전체가 롤백되어, 첫 질문 없는
     * "빈 껍데기" IN_PROGRESS Interview Session이 DB에 남지 않는다.
     */
    @Transactional
    public InterviewStartResponse startInterview(Long userId, InterviewStartRequest request) {
        LocalDateTime now = LocalDateTime.now();

        SpeakingSession session = new SpeakingSession(
                userRepository.getReferenceById(userId),
                SessionType.INTERVIEW,
                SessionStatus.IN_PROGRESS,
                now,
                now
        );
        speakingSessionRepository.save(session);

        InterviewSetting setting = new InterviewSetting(
                session,
                request.jobRole(),
                request.interviewType(),
                request.difficulty(),
                request.additionalRequest(),
                request.subtitleMode(),
                TARGET_COUNTRY,
                TARGET_LANGUAGE,
                FEEDBACK_LANGUAGE,
                now
        );
        interviewSettingRepository.save(setting);

        InterviewFirstQuestionResult aiResult = interviewAiService.generateFirstQuestion(setting);

        InterviewQuestion firstQuestion = new InterviewQuestion(
                session,
                null,
                null,
                QuestionKind.INITIAL,
                aiResult.questionText(),
                FIRST_QUESTION_SEQUENCE_NO,
                QuestionStatus.ASKED,
                now
        );
        interviewQuestionRepository.save(firstQuestion);

        appendMessage(session, Speaker.AI, QUESTION_MESSAGE_TYPE, aiResult.questionText());

        return new InterviewStartResponse(
                session.getId(),
                session.getStatus(),
                InterviewQuestionResponse.from(firstQuestion)
        );
    }

    @Transactional
    public InterviewAnswerResponse submitAudioAnswer(
            Long sessionId,
            Long questionId,
            Long userId,
            Resource audioResource
    ) {
        SpeakingSession session = loadOwnedInProgressInterview(sessionId, userId);
        InterviewQuestion question = loadAnswerableQuestion(sessionId, questionId);
        assertNotAlreadyAnswered(question);

        String answerText = aiClient.transcribe(audioResource);
        if (answerText == null || answerText.isBlank()) {
            throw new AiServiceException("음성에서 답변 내용을 인식하지 못했습니다.");
        }
        return evaluateAndSave(session, question, answerText.trim());
    }

    @Transactional
    public InterviewAnswerResponse submitTextAnswer(
            Long sessionId,
            Long questionId,
            Long userId,
            String answerText
    ) {
        SpeakingSession session = loadOwnedInProgressInterview(sessionId, userId);
        InterviewQuestion question = loadAnswerableQuestion(sessionId, questionId);
        assertNotAlreadyAnswered(question);
        return evaluateAndSave(session, question, answerText.trim());
    }

    /**
     * 면접 정상 종료 + 종합 Feedback 생성을 하나의 성공 단위로 처리한다.
     * 미답변(ASKED) 질문이 남아 있으면 완료할 수 없다 — Phase 3 progression 설계상 이는
     * "더 진행할 질문이 없다"와 동치다(마지막 답변 처리 시 진행 여부를 이미 Backend가 결정했으므로).
     * 종합 Feedback AI 또는 저장이 실패하면 이 메서드 전체가 롤백되어, session만 COMPLETED로
     * 남고 Feedback이 없는 상태가 생기지 않는다.
     */
    @Transactional
    public SessionCompletionResponse completeInterview(Long sessionId, Long userId) {
        SpeakingSession session = loadOwnedInProgressInterview(sessionId, userId);

        if (interviewQuestionRepository.existsBySession_IdAndStatus(sessionId, QuestionStatus.ASKED)) {
            throw new InterviewAnswerConflictException("아직 답변하지 않은 면접 질문이 있습니다.");
        }

        List<InterviewQuestion> questions = interviewQuestionRepository
                .findBySession_IdOrderBySequenceNoAsc(sessionId);
        if (questions.isEmpty()) {
            throw new InterviewAnswerConflictException("아직 진행한 면접 질문이 없습니다.");
        }

        session.complete(LocalDateTime.now());
        generateAndSaveFinalFeedback(session, questions);

        return new SessionCompletionResponse(
                session.getId(),
                session.getStatus(),
                session.getEndedAt(),
                session.getDurationSeconds(),
                com.aijapanese.speaking.conversation.entity.GenerationStatus.COMPLETED
        );
    }

    /**
     * 저장된 종합 Feedback과 답변별 Feedback을 조회한다. AI를 다시 호출하지 않는다.
     */
    @Transactional(readOnly = true)
    public InterviewFeedbackResponse getFeedback(Long sessionId, Long userId) {
        loadOwnedInterviewSession(sessionId, userId);

        InterviewFeedback feedback = interviewFeedbackRepository.findBySession_Id(sessionId)
                .orElseThrow(() -> new InterviewFeedbackNotFoundException("면접 종합 피드백을 찾을 수 없습니다."));

        List<InterviewQuestion> questions = interviewQuestionRepository
                .findBySession_IdOrderBySequenceNoAsc(sessionId);
        List<InterviewAnswerFeedbackResponse> answers = loadAnswerBundles(questions).stream()
                .map(bundle -> new InterviewAnswerFeedbackResponse(
                        bundle.question().getQuestionText(),
                        bundle.answer().getAnswerText(),
                        bundle.feedback().getOverallScore(),
                        toScoreResponses(bundle.scores()),
                        bundle.feedback().getStrengths(),
                        bundle.feedback().getWeaknesses(),
                        bundle.feedback().getCoachingSummary(),
                        bundle.feedback().getImprovementTips(),
                        bundle.feedback().getImprovedAnswer(),
                        bundle.feedback().getRubricVersion(),
                        bundle.feedback().getGenerationStatus()
                ))
                .toList();

        InterviewOverallFeedbackResponse overall = new InterviewOverallFeedbackResponse(
                feedback.getOverallScore(),
                feedback.getSummary(),
                feedback.getStrengths(),
                feedback.getImprovements(),
                feedback.getEvaluatedAnswerCount(),
                feedback.getRubricVersion(),
                feedback.getGenerationStatus()
        );

        return new InterviewFeedbackResponse(sessionId, overall, answers);
    }

    private void generateAndSaveFinalFeedback(SpeakingSession session, List<InterviewQuestion> questions) {
        InterviewSetting setting = interviewSettingRepository.findBySession_Id(session.getId())
                .orElseThrow(() -> new IllegalStateException("면접 설정을 찾을 수 없습니다."));

        List<AnswerBundle> bundles = loadAnswerBundles(questions);
        List<InterviewEvaluationScore> allScores = bundles.stream()
                .flatMap(bundle -> bundle.scores().stream())
                .toList();

        BigDecimal overallScore = calculateInterviewOverallScore(allScores);
        Map<EvaluationCriterion, BigDecimal> criterionAverages = calculateCriterionAverages(allScores);

        List<InterviewAnsweredQuestionSummary> summaries = bundles.stream()
                .map(bundle -> new InterviewAnsweredQuestionSummary(
                        bundle.question().getSequenceNo(),
                        bundle.question().getQuestionKind().name(),
                        bundle.question().getQuestionText(),
                        bundle.answer().getAnswerText(),
                        bundle.feedback().getEvaluationSummary(),
                        bundle.feedback().getStrengths(),
                        bundle.feedback().getWeaknesses(),
                        bundle.feedback().getOverallScore()
                ))
                .toList();

        InterviewFinalFeedbackAiResult aiResult = finalFeedbackAiService.summarize(
                setting, summaries, overallScore, criterionAverages
        );

        InterviewFeedback feedback = new InterviewFeedback(
                session,
                overallScore,
                aiResult.summary(),
                aiResult.strengths(),
                aiResult.improvements(),
                bundles.size(),
                RUBRIC_VERSION,
                GenerationStatus.COMPLETED,
                LocalDateTime.now()
        );
        interviewFeedbackRepository.save(feedback);
    }

    /**
     * ANSWERED 질문마다 답변·평가·점수를 묶어 조회한다. Phase 3 설계상 ANSWERED 질문에는
     * 항상 답변·평가가 존재한다(평가 실패 시 답변 자체가 저장되지 않고 롤백되므로).
     */
    private List<AnswerBundle> loadAnswerBundles(List<InterviewQuestion> questions) {
        List<AnswerBundle> bundles = new ArrayList<>();
        for (InterviewQuestion question : questions) {
            if (question.getStatus() != QuestionStatus.ANSWERED) {
                continue;
            }
            InterviewAnswer answer = interviewAnswerRepository.findByQuestion_Id(question.getId())
                    .orElseThrow(() -> new IllegalStateException("면접 답변을 찾을 수 없습니다."));
            InterviewAnswerFeedback feedback = interviewAnswerFeedbackRepository.findByAnswer_Id(answer.getId())
                    .orElseThrow(() -> new IllegalStateException("면접 답변 평가를 찾을 수 없습니다."));
            List<InterviewEvaluationScore> scores = interviewEvaluationScoreRepository
                    .findByAnswerFeedback_IdOrderByIdAsc(feedback.getId());
            bundles.add(new AnswerBundle(question, answer, feedback, scores));
        }
        return bundles;
    }

    private List<InterviewEvaluationScoreResponse> toScoreResponses(List<InterviewEvaluationScore> scores) {
        return scores.stream()
                .map(score -> new InterviewEvaluationScoreResponse(
                        score.getCriterion(), score.getScore(), score.getFeedback(), score.isApplicable()
                ))
                .toList();
    }

    /**
     * Phase 2와 동일한 방식(applicable score 단순 평균, HALF_UP, scale 2)을 면접 전체로 확장한다.
     */
    private BigDecimal calculateInterviewOverallScore(List<InterviewEvaluationScore> scores) {
        List<BigDecimal> applicableScores = scores.stream()
                .filter(InterviewEvaluationScore::isApplicable)
                .map(InterviewEvaluationScore::getScore)
                .toList();
        if (applicableScores.isEmpty()) {
            throw new AiServiceException("적용 가능한 면접 평가 항목이 없습니다.");
        }
        BigDecimal sum = applicableScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(applicableScores.size()), 2, RoundingMode.HALF_UP);
    }

    private Map<EvaluationCriterion, BigDecimal> calculateCriterionAverages(List<InterviewEvaluationScore> scores) {
        Map<EvaluationCriterion, List<BigDecimal>> byCriterion = new LinkedHashMap<>();
        for (InterviewEvaluationScore score : scores) {
            if (!score.isApplicable()) {
                continue;
            }
            byCriterion.computeIfAbsent(score.getCriterion(), key -> new ArrayList<>()).add(score.getScore());
        }

        Map<EvaluationCriterion, BigDecimal> averages = new LinkedHashMap<>();
        byCriterion.forEach((criterion, criterionScores) -> {
            BigDecimal sum = criterionScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            averages.put(criterion, sum.divide(BigDecimal.valueOf(criterionScores.size()), 2, RoundingMode.HALF_UP));
        });
        return averages;
    }

    private InterviewAnswerResponse evaluateAndSave(
            SpeakingSession session,
            InterviewQuestion question,
            String answerText
    ) {
        InterviewSetting setting = interviewSettingRepository.findBySession_Id(session.getId())
                .orElseThrow(() -> new IllegalStateException("면접 설정을 찾을 수 없습니다."));

        InterviewAnswerEvaluationAiResult result = answerEvaluationAiService.evaluate(setting, question, answerText);
        BigDecimal overallScore = calculateOverallScore(result.evaluations());
        List<InterviewQuestion> existingQuestions = interviewQuestionRepository
                .findBySession_IdOrderBySequenceNoAsc(session.getId());
        validateProgressionState(existingQuestions);

        boolean followUpAllowed = question.getQuestionKind() == QuestionKind.INITIAL
                && existingQuestions.size() < MAX_TOTAL_QUESTIONS
                && !interviewQuestionRepository.existsByParentQuestion_Id(question.getId());
        InterviewAnswerCoachingAiResult coaching = answerCoachingAiService.coach(
                setting, question, answerText, result, followUpAllowed
        );
        ProgressionPlan progressionPlan = planProgression(
                setting, question, existingQuestions, coaching, followUpAllowed
        );
        LocalDateTime now = LocalDateTime.now();

        question.applyAnalysis(
                result.questionIntent(),
                result.questionType(),
                result.coreCompetencies(),
                result.starRecommended()
        );
        question.markAnswered();

        appendMessage(session, Speaker.USER, ANSWER_MESSAGE_TYPE, answerText);
        InterviewAnswer answer = interviewAnswerRepository.save(new InterviewAnswer(question, answerText, now));
        InterviewAnswerFeedback feedback = new InterviewAnswerFeedback(
                answer,
                overallScore,
                result.summary(),
                result.strengths(),
                result.weaknesses(),
                RUBRIC_VERSION,
                GenerationStatus.COMPLETED,
                now
        );
        feedback.applyCoaching(
                coaching.coachingSummary(),
                coaching.improvementTips(),
                coaching.modelAnswer()
        );
        interviewAnswerFeedbackRepository.save(feedback);

        List<InterviewEvaluationScoreResponse> scoreResponses = result.evaluations().stream()
                .map(evaluation -> {
                    InterviewEvaluationScore score = interviewEvaluationScoreRepository.save(
                            new InterviewEvaluationScore(
                                    feedback,
                                    evaluation.criterion(),
                                    evaluation.score(),
                                    evaluation.comment(),
                                    evaluation.applicable()
                            )
                    );
                    return new InterviewEvaluationScoreResponse(
                            score.getCriterion(),
                            score.getScore(),
                            score.getFeedback(),
                            score.isApplicable()
                    );
                })
                .toList();

        InterviewAnswerEvaluationResponse evaluationResponse = new InterviewAnswerEvaluationResponse(
                overallScore,
                result.summary(),
                scoreResponses,
                result.strengths(),
                result.weaknesses(),
                coaching.coachingSummary(),
                coaching.improvementTips(),
                coaching.modelAnswer(),
                RUBRIC_VERSION,
                GenerationStatus.COMPLETED,
                result.starRecommended()
        );

        InterviewQuestionResponse nextQuestion = saveNextQuestion(
                session, answer, progressionPlan, existingQuestions.size() + 1, now
        );
        return new InterviewAnswerResponse(
                answer.getId(), answerText, evaluationResponse, nextQuestion, progressionPlan == null
        );
    }

    private ProgressionPlan planProgression(
            InterviewSetting setting,
            InterviewQuestion currentQuestion,
            List<InterviewQuestion> existingQuestions,
            InterviewAnswerCoachingAiResult coaching,
            boolean followUpAllowed
    ) {
        if (followUpAllowed && coaching.followUpNeeded()) {
            return new ProgressionPlan(
                    QuestionKind.FOLLOW_UP,
                    coaching.followUpQuestion(),
                    currentQuestion
            );
        }

        long initialCount = existingQuestions.stream()
                .filter(question -> question.getQuestionKind() == QuestionKind.INITIAL)
                .count();
        if (initialCount >= MAX_INITIAL_QUESTIONS || existingQuestions.size() >= MAX_TOTAL_QUESTIONS) {
            return null;
        }

        InterviewFirstQuestionResult nextInitial = interviewAiService.generateNextInitialQuestion(
                setting, existingQuestions
        );
        return new ProgressionPlan(QuestionKind.INITIAL, nextInitial.questionText(), null);
    }

    private InterviewQuestionResponse saveNextQuestion(
            SpeakingSession session,
            InterviewAnswer answer,
            ProgressionPlan plan,
            int sequenceNo,
            LocalDateTime now
    ) {
        if (plan == null) {
            return null;
        }

        InterviewQuestion nextQuestion = new InterviewQuestion(
                session,
                plan.parentQuestion(),
                plan.questionKind() == QuestionKind.FOLLOW_UP ? answer : null,
                plan.questionKind(),
                plan.questionText(),
                sequenceNo,
                QuestionStatus.ASKED,
                now
        );
        interviewQuestionRepository.save(nextQuestion);
        appendMessage(session, Speaker.AI, QUESTION_MESSAGE_TYPE, nextQuestion.getQuestionText());
        return InterviewQuestionResponse.from(nextQuestion);
    }

    private void validateProgressionState(List<InterviewQuestion> questions) {
        long initialCount = questions.stream()
                .filter(question -> question.getQuestionKind() == QuestionKind.INITIAL)
                .count();
        if (initialCount > MAX_INITIAL_QUESTIONS || questions.size() > MAX_TOTAL_QUESTIONS) {
            throw new InterviewAnswerConflictException("면접 질문 진행 상태가 허용 범위를 초과했습니다.");
        }
    }

    private BigDecimal calculateOverallScore(
            List<InterviewAnswerEvaluationAiResult.CriterionEvaluation> evaluations
    ) {
        List<BigDecimal> applicableScores = evaluations.stream()
                .filter(InterviewAnswerEvaluationAiResult.CriterionEvaluation::applicable)
                .map(InterviewAnswerEvaluationAiResult.CriterionEvaluation::score)
                .toList();
        if (applicableScores.isEmpty()) {
            throw new AiServiceException("적용 가능한 면접 평가 항목이 없습니다.");
        }

        BigDecimal sum = applicableScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(applicableScores.size()), 2, RoundingMode.HALF_UP);
    }

    private SpeakingSession loadOwnedInProgressInterview(Long sessionId, Long userId) {
        SpeakingSession session = speakingSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new SpeakingSessionNotFoundException("세션을 찾을 수 없습니다."));
        if (!session.getUser().getId().equals(userId)) {
            throw new SpeakingSessionAccessDeniedException("본인의 세션만 접근할 수 있습니다.");
        }
        if (session.getSessionType() != SessionType.INTERVIEW) {
            throw new SpeakingSessionNotFoundException("세션을 찾을 수 없습니다.");
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new SpeakingSessionNotInProgressException("진행 중인 면접에서만 답변할 수 있습니다.");
        }
        return session;
    }

    /**
     * complete 이후(COMPLETED) 상태에서도 조회 가능해야 하는 getFeedback 전용 loader다.
     * IN_PROGRESS를 요구하지 않고 행 잠금도 걸지 않는다(읽기 전용).
     */
    private SpeakingSession loadOwnedInterviewSession(Long sessionId, Long userId) {
        SpeakingSession session = speakingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SpeakingSessionNotFoundException("세션을 찾을 수 없습니다."));
        if (!session.getUser().getId().equals(userId)) {
            throw new SpeakingSessionAccessDeniedException("본인의 세션만 접근할 수 있습니다.");
        }
        if (session.getSessionType() != SessionType.INTERVIEW) {
            throw new SpeakingSessionNotFoundException("세션을 찾을 수 없습니다.");
        }
        return session;
    }

    private InterviewQuestion loadAnswerableQuestion(Long sessionId, Long questionId) {
        InterviewQuestion question = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new InterviewQuestionNotFoundException("면접 질문을 찾을 수 없습니다."));
        if (!question.getSession().getId().equals(sessionId)) {
            throw new InterviewQuestionNotFoundException("면접 질문을 찾을 수 없습니다.");
        }
        if (question.getStatus() != QuestionStatus.ASKED) {
            throw new InterviewAnswerConflictException("현재 답변할 수 없는 면접 질문입니다.");
        }
        return question;
    }

    private void assertNotAlreadyAnswered(InterviewQuestion question) {
        if (interviewAnswerRepository.existsByQuestion_Id(question.getId())) {
            throw new InterviewAnswerConflictException("이미 답변한 면접 질문입니다.");
        }
    }

    private SpeakingMessage appendMessage(
            SpeakingSession session,
            Speaker speaker,
            String messageType,
            String content
    ) {
        int sequenceNo = (int) speakingMessageRepository.countBySession_Id(session.getId()) + 1;
        return speakingMessageRepository.save(new SpeakingMessage(
                session,
                sequenceNo,
                speaker,
                messageType,
                content,
                LocalDateTime.now()
        ));
    }

    private record ProgressionPlan(
            QuestionKind questionKind,
            String questionText,
            InterviewQuestion parentQuestion
    ) {
    }

    private record AnswerBundle(
            InterviewQuestion question,
            InterviewAnswer answer,
            InterviewAnswerFeedback feedback,
            List<InterviewEvaluationScore> scores
    ) {
    }
}
