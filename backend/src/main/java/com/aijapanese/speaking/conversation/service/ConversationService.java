package com.aijapanese.speaking.conversation.service;

import com.aijapanese.speaking.conversation.ai.ConversationAiReply;
import com.aijapanese.speaking.conversation.ai.ConversationAiService;
import com.aijapanese.speaking.conversation.ai.ConversationFeedbackAiResult;
import com.aijapanese.speaking.conversation.ai.ConversationFeedbackAiService;
import com.aijapanese.speaking.conversation.dto.ConversationCorrectionResponse;
import com.aijapanese.speaking.conversation.dto.ConversationFeedbackResponse;
import com.aijapanese.speaking.conversation.dto.ConversationStartRequest;
import com.aijapanese.speaking.conversation.dto.SessionCompletionResponse;
import com.aijapanese.speaking.conversation.entity.ConversationCorrection;
import com.aijapanese.speaking.conversation.entity.ConversationFeedback;
import com.aijapanese.speaking.conversation.entity.ConversationSetting;
import com.aijapanese.speaking.conversation.entity.GenerationStatus;
import com.aijapanese.speaking.conversation.exception.ConversationFeedbackNotFoundException;
import com.aijapanese.speaking.conversation.repository.ConversationCorrectionRepository;
import com.aijapanese.speaking.conversation.repository.ConversationFeedbackRepository;
import com.aijapanese.speaking.conversation.repository.ConversationSettingRepository;
import com.aijapanese.speaking.speaking.dto.SpeakingSessionResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    private static final String CONVERSATION_MESSAGE_TYPE = "CONVERSATION";

    private final SpeakingSessionRepository speakingSessionRepository;
    private final ConversationSettingRepository conversationSettingRepository;
    private final SpeakingMessageRepository speakingMessageRepository;
    private final ConversationFeedbackRepository conversationFeedbackRepository;
    private final ConversationCorrectionRepository conversationCorrectionRepository;
    private final UserRepository userRepository;
    private final ConversationAiService conversationAiService;
    private final ConversationFeedbackAiService conversationFeedbackAiService;

    public ConversationService(
            SpeakingSessionRepository speakingSessionRepository,
            ConversationSettingRepository conversationSettingRepository,
            SpeakingMessageRepository speakingMessageRepository,
            ConversationFeedbackRepository conversationFeedbackRepository,
            ConversationCorrectionRepository conversationCorrectionRepository,
            UserRepository userRepository,
            ConversationAiService conversationAiService,
            ConversationFeedbackAiService conversationFeedbackAiService
    ) {
        this.speakingSessionRepository = speakingSessionRepository;
        this.conversationSettingRepository = conversationSettingRepository;
        this.speakingMessageRepository = speakingMessageRepository;
        this.conversationFeedbackRepository = conversationFeedbackRepository;
        this.conversationCorrectionRepository = conversationCorrectionRepository;
        this.userRepository = userRepository;
        this.conversationAiService = conversationAiService;
        this.conversationFeedbackAiService = conversationFeedbackAiService;
    }

    @Transactional
    public SpeakingSessionResponse startConversation(Long userId, ConversationStartRequest request) {
        LocalDateTime now = LocalDateTime.now();

        SpeakingSession session = new SpeakingSession(
                userRepository.getReferenceById(userId),
                SessionType.CONVERSATION,
                SessionStatus.IN_PROGRESS,
                now,
                now
        );
        speakingSessionRepository.save(session);

        ConversationSetting setting = new ConversationSetting(
                session,
                request.situation(),
                request.partnerRole(),
                request.partnerPersonality(),
                request.situationDescription(),
                request.difficulty(),
                request.subtitleMode(),
                now
        );
        conversationSettingRepository.save(setting);

        return SpeakingSessionResponse.from(session);
    }

    /**
     * 사용자 발화를 speaking_messages에 저장한다. (STT 이후 텍스트 기준)
     */
    @Transactional
    public SpeakingMessage recordUserMessage(Long sessionId, Long userId, String content) {
        SpeakingSession session = loadOwnedInProgressSession(sessionId, userId);
        return appendMessage(session, Speaker.USER, content);
    }

    /**
     * 텍스트 기준 한 Turn을 처리한다: User Message 저장 → Conversation LLM → AI Message 저장.
     * OpenAI 호출은 ConversationAiService(Domain AI Service)에게 위임한다.
     */
    @Transactional
    public TurnResult processTextTurn(Long sessionId, Long userId, String userMessageContent) {
        loadOwnedInProgressSession(sessionId, userId);

        List<SpeakingMessage> recentHistory = speakingMessageRepository
                .findTop10BySession_IdOrderBySequenceNoDesc(sessionId);
        Collections.reverse(recentHistory);

        SpeakingMessage userMessage = recordUserMessage(sessionId, userId, userMessageContent);
        SpeakingSession session = userMessage.getSession();

        ConversationSetting setting = conversationSettingRepository.findBySession_Id(sessionId)
                .orElseThrow(() -> new IllegalStateException("일반 회화 설정을 찾을 수 없습니다."));

        ConversationAiReply reply = conversationAiService.generateReply(setting, recentHistory, userMessageContent);

        SpeakingMessage aiMessage = appendMessage(session, Speaker.AI, reply.japaneseText());

        return new TurnResult(userMessage, aiMessage, reply.koreanTranslation());
    }

    /**
     * 음성 기준 한 Turn을 처리한다: STT → processTextTurn → TTS.
     */
    @Transactional
    public AudioTurnResult processAudioTurn(Long sessionId, Long userId, Resource audioResource) {
        loadOwnedInProgressSession(sessionId, userId);

        String transcribedText = conversationAiService.transcribe(audioResource);
        TurnResult turnResult = processTextTurn(sessionId, userId, transcribedText);
        byte[] aiAudio = conversationAiService.synthesizeSpeech(turnResult.aiMessage().getContent());

        return new AudioTurnResult(turnResult.userMessage(), turnResult.aiMessage(), turnResult.aiKoreanSubtitle(), aiAudio);
    }

    @Transactional
    public SessionCompletionResponse completeConversation(Long sessionId, Long userId) {
        SpeakingSession session = loadOwnedInProgressSession(sessionId, userId);

        session.complete(LocalDateTime.now());

        GenerationStatus feedbackStatus = generateAndSaveFeedback(session);

        return new SessionCompletionResponse(
                session.getId(),
                session.getStatus(),
                session.getEndedAt(),
                session.getDurationSeconds(),
                feedbackStatus
        );
    }

    /**
     * 일반회화 중도 종료. complete와 달리 Feedback AI를 호출하지 않는다.
     */
    @Transactional
    public SessionCompletionResponse abortConversation(Long sessionId, Long userId) {
        SpeakingSession session = loadOwnedInProgressSession(sessionId, userId);

        if (session.getSessionType() != SessionType.CONVERSATION) {
            throw new SpeakingSessionNotFoundException("세션을 찾을 수 없습니다.");
        }

        session.abort(LocalDateTime.now());

        return new SessionCompletionResponse(
                session.getId(),
                session.getStatus(),
                session.getEndedAt(),
                session.getDurationSeconds(),
                null
        );
    }

    @Transactional(readOnly = true)
    public ConversationFeedbackResponse getFeedback(Long sessionId, Long userId) {
        loadOwnedSession(sessionId, userId);

        ConversationFeedback feedback = conversationFeedbackRepository.findBySession_Id(sessionId)
                .orElseThrow(() -> new ConversationFeedbackNotFoundException("피드백을 찾을 수 없습니다."));

        List<ConversationCorrectionResponse> corrections = conversationCorrectionRepository
                .findByFeedback_IdOrderByDisplayOrderAsc(feedback.getId())
                .stream()
                .map(ConversationCorrectionResponse::from)
                .toList();

        return new ConversationFeedbackResponse(
                sessionId,
                feedback.getGenerationStatus(),
                feedback.getSummary(),
                feedback.getNaturalnessComment(),
                feedback.getGrammarComment(),
                feedback.getVocabularyComment(),
                feedback.getStrengths(),
                feedback.getNextTip(),
                corrections
        );
    }

    private GenerationStatus generateAndSaveFeedback(SpeakingSession session) {
        List<SpeakingMessage> transcript = speakingMessageRepository
                .findBySession_IdOrderBySequenceNoAsc(session.getId());

        if (transcript.isEmpty()) {
            saveFailedFeedback(session);
            return GenerationStatus.FAILED;
        }

        ConversationSetting setting = conversationSettingRepository.findBySession_Id(session.getId()).orElse(null);

        try {
            ConversationFeedbackAiResult result = conversationFeedbackAiService.generateFeedback(setting, transcript);

            ConversationFeedback feedback = new ConversationFeedback(
                    session,
                    GenerationStatus.COMPLETED,
                    result.summary(),
                    result.naturalnessComment(),
                    result.grammarComment(),
                    result.vocabularyComment(),
                    result.strengths(),
                    result.nextTip(),
                    LocalDateTime.now()
            );
            conversationFeedbackRepository.save(feedback);

            saveCorrections(feedback, transcript, result.corrections());

            return GenerationStatus.COMPLETED;
        } catch (RuntimeException e) {
            log.warn("Conversation feedback generation failed for session {}", session.getId(), e);
            saveFailedFeedback(session);
            return GenerationStatus.FAILED;
        }
    }

    private void saveCorrections(
            ConversationFeedback feedback,
            List<SpeakingMessage> transcript,
            List<ConversationFeedbackAiResult.Correction> corrections
    ) {
        Map<Integer, SpeakingMessage> userMessagesBySequence = transcript.stream()
                .filter(message -> message.getSpeaker() == Speaker.USER)
                .collect(Collectors.toMap(SpeakingMessage::getSequenceNo, message -> message));

        int order = 0;
        for (ConversationFeedbackAiResult.Correction correction : corrections) {
            SpeakingMessage relatedMessage = correction.relatedUserMessageSequenceNo() != null
                    ? userMessagesBySequence.get(correction.relatedUserMessageSequenceNo())
                    : null;

            ConversationCorrection entity = new ConversationCorrection(
                    feedback,
                    relatedMessage,
                    correction.category(),
                    correction.originalExpression(),
                    correction.suggestedExpression(),
                    correction.reading(),
                    correction.koreanTranslation(),
                    correction.explanation(),
                    order++
            );
            conversationCorrectionRepository.save(entity);
        }
    }

    private void saveFailedFeedback(SpeakingSession session) {
        ConversationFeedback feedback = new ConversationFeedback(
                session, GenerationStatus.FAILED, null, null, null, null, null, null, null
        );
        conversationFeedbackRepository.save(feedback);
    }

    private SpeakingMessage appendMessage(SpeakingSession session, Speaker speaker, String content) {
        long nextSequenceNo = speakingMessageRepository.countBySession_Id(session.getId()) + 1;

        SpeakingMessage message = new SpeakingMessage(
                session,
                (int) nextSequenceNo,
                speaker,
                CONVERSATION_MESSAGE_TYPE,
                content,
                LocalDateTime.now()
        );

        return speakingMessageRepository.save(message);
    }

    private SpeakingSession loadOwnedSession(Long sessionId, Long userId) {
        SpeakingSession session = speakingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SpeakingSessionNotFoundException("세션을 찾을 수 없습니다."));

        if (!session.getUser().getId().equals(userId)) {
            throw new SpeakingSessionAccessDeniedException("본인의 세션만 접근할 수 있습니다.");
        }

        return session;
    }

    /**
     * IN_PROGRESS 세션을 변경하는 모든 작업(turn 추가, complete, abort)의 진입점이다.
     * 세션 row를 잠가(findByIdForUpdate) 같은 세션에 대한 동시 요청이 순차적으로 처리되도록 해,
     * speaking_messages의 sequence_no가 동시 요청으로 겹치는 것을 막는다.
     */
    private SpeakingSession loadOwnedInProgressSession(Long sessionId, Long userId) {
        SpeakingSession session = speakingSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new SpeakingSessionNotFoundException("세션을 찾을 수 없습니다."));

        if (!session.getUser().getId().equals(userId)) {
            throw new SpeakingSessionAccessDeniedException("본인의 세션만 접근할 수 있습니다.");
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new SpeakingSessionNotInProgressException("진행 중인 세션에서만 가능한 작업입니다.");
        }

        return session;
    }

    public record TurnResult(SpeakingMessage userMessage, SpeakingMessage aiMessage, String aiKoreanSubtitle) {
    }

    public record AudioTurnResult(
            SpeakingMessage userMessage,
            SpeakingMessage aiMessage,
            String aiKoreanSubtitle,
            byte[] aiAudio
    ) {
    }
}
