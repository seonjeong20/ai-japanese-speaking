package com.aijapanese.speaking.history.service;

import com.aijapanese.speaking.conversation.dto.ConversationFeedbackResponse;
import com.aijapanese.speaking.conversation.entity.ConversationSetting;
import com.aijapanese.speaking.conversation.exception.ConversationFeedbackNotFoundException;
import com.aijapanese.speaking.conversation.repository.ConversationSettingRepository;
import com.aijapanese.speaking.conversation.service.ConversationService;
import com.aijapanese.speaking.history.dto.HistoryDetailResponse;
import com.aijapanese.speaking.history.dto.HistoryListItemResponse;
import com.aijapanese.speaking.history.dto.HistoryListResponse;
import com.aijapanese.speaking.history.dto.HistoryMessageResponse;
import com.aijapanese.speaking.interview.dto.InterviewFeedbackResponse;
import com.aijapanese.speaking.interview.entity.InterviewSetting;
import com.aijapanese.speaking.interview.exception.InterviewFeedbackNotFoundException;
import com.aijapanese.speaking.interview.repository.InterviewFeedbackRepository;
import com.aijapanese.speaking.interview.repository.InterviewSettingRepository;
import com.aijapanese.speaking.interview.service.InterviewService;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotFoundException;
import com.aijapanese.speaking.speaking.repository.SpeakingMessageRepository;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * My History / History Detail 전용 조회 서비스.
 *
 * 새로운 테이블을 만들지 않고 speaking_sessions + 기존 conversation/interview 테이블을
 * 읽기 전용으로 조합한다. AI를 다시 호출하지 않으며, feedback 조회는 ConversationService/
 * InterviewService의 기존 getFeedback(소유권 검증 포함)을 그대로 재사용한다.
 */
@Service
public class HistoryService {

    private static final String DEFAULT_CONVERSATION_TITLE = "일반 회화";
    private static final String DEFAULT_INTERVIEW_TITLE = "면접 회화";

    private final SpeakingSessionRepository speakingSessionRepository;
    private final SpeakingMessageRepository speakingMessageRepository;
    private final ConversationSettingRepository conversationSettingRepository;
    private final InterviewSettingRepository interviewSettingRepository;
    private final InterviewFeedbackRepository interviewFeedbackRepository;
    private final ConversationService conversationService;
    private final InterviewService interviewService;

    public HistoryService(
            SpeakingSessionRepository speakingSessionRepository,
            SpeakingMessageRepository speakingMessageRepository,
            ConversationSettingRepository conversationSettingRepository,
            InterviewSettingRepository interviewSettingRepository,
            InterviewFeedbackRepository interviewFeedbackRepository,
            ConversationService conversationService,
            InterviewService interviewService
    ) {
        this.speakingSessionRepository = speakingSessionRepository;
        this.speakingMessageRepository = speakingMessageRepository;
        this.conversationSettingRepository = conversationSettingRepository;
        this.interviewSettingRepository = interviewSettingRepository;
        this.interviewFeedbackRepository = interviewFeedbackRepository;
        this.conversationService = conversationService;
        this.interviewService = interviewService;
    }

    @Transactional(readOnly = true)
    public HistoryListResponse getHistory(Long userId) {
        List<SpeakingSession> sessions = speakingSessionRepository
                .findByUser_IdAndStatusOrderByStartedAtDesc(userId, SessionStatus.COMPLETED);

        return new HistoryListResponse(sessions.stream().map(this::toListItem).toList());
    }

    private HistoryListItemResponse toListItem(SpeakingSession session) {
        if (session.getSessionType() == SessionType.CONVERSATION) {
            String title = conversationSettingRepository.findBySession_Id(session.getId())
                    .map(ConversationSetting::getSituation)
                    .filter(situation -> situation != null && !situation.isBlank())
                    .orElse(DEFAULT_CONVERSATION_TITLE);
            return new HistoryListItemResponse(
                    session.getId(), session.getSessionType(), session.getStatus(), title,
                    session.getStartedAt(), session.getDurationSeconds(), null
            );
        }

        String title = interviewSettingRepository.findBySession_Id(session.getId())
                .map(InterviewSetting::getJobRole)
                .orElse(DEFAULT_INTERVIEW_TITLE);
        BigDecimal overallScore = interviewFeedbackRepository.findBySession_Id(session.getId())
                .map(com.aijapanese.speaking.interview.entity.InterviewFeedback::getOverallScore)
                .orElse(null);
        return new HistoryListItemResponse(
                session.getId(), session.getSessionType(), session.getStatus(), title,
                session.getStartedAt(), session.getDurationSeconds(), overallScore
        );
    }

    @Transactional(readOnly = true)
    public HistoryDetailResponse getHistoryDetail(Long sessionId, Long userId) {
        SpeakingSession session = loadOwnedSession(sessionId, userId);

        if (session.getSessionType() == SessionType.CONVERSATION) {
            return buildConversationDetail(session, userId);
        }
        return buildInterviewDetail(session, userId);
    }

    private HistoryDetailResponse buildConversationDetail(SpeakingSession session, Long userId) {
        ConversationSetting setting = conversationSettingRepository.findBySession_Id(session.getId())
                .orElse(null);

        List<HistoryMessageResponse> transcript = speakingMessageRepository
                .findBySession_IdOrderBySequenceNoAsc(session.getId())
                .stream()
                .map(HistoryMessageResponse::from)
                .toList();

        ConversationFeedbackResponse feedback;
        try {
            feedback = conversationService.getFeedback(session.getId(), userId);
        } catch (ConversationFeedbackNotFoundException e) {
            feedback = null;
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        if (setting != null) {
            settings.put("situation", setting.getSituation());
            settings.put("partnerRole", setting.getPartnerRole());
            settings.put("partnerPersonality", setting.getPartnerPersonality());
            settings.put("situationDescription", setting.getSituationDescription());
            settings.put("difficulty", setting.getDifficulty().name());
            settings.put("subtitleMode", setting.getSubtitleMode().name());
        }

        return new HistoryDetailResponse(
                session.getId(), session.getSessionType(), session.getStatus(),
                session.getStartedAt(), session.getEndedAt(), session.getDurationSeconds(),
                settings, transcript, feedback, null
        );
    }

    private HistoryDetailResponse buildInterviewDetail(SpeakingSession session, Long userId) {
        InterviewSetting setting = interviewSettingRepository.findBySession_Id(session.getId())
                .orElse(null);

        List<HistoryMessageResponse> transcript = speakingMessageRepository
                .findBySession_IdOrderBySequenceNoAsc(session.getId())
                .stream()
                .map(HistoryMessageResponse::from)
                .toList();

        InterviewFeedbackResponse feedback;
        try {
            feedback = interviewService.getFeedback(session.getId(), userId);
        } catch (InterviewFeedbackNotFoundException e) {
            feedback = null;
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        if (setting != null) {
            settings.put("jobRole", setting.getJobRole());
            settings.put("interviewType", setting.getInterviewType());
            settings.put("difficulty", setting.getDifficulty().name());
            settings.put("subtitleMode", setting.getSubtitleMode().name());
        }

        return new HistoryDetailResponse(
                session.getId(), session.getSessionType(), session.getStatus(),
                session.getStartedAt(), session.getEndedAt(), session.getDurationSeconds(),
                settings, transcript, null, feedback
        );
    }

    private SpeakingSession loadOwnedSession(Long sessionId, Long userId) {
        SpeakingSession session = speakingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SpeakingSessionNotFoundException("세션을 찾을 수 없습니다."));

        if (!session.getUser().getId().equals(userId)) {
            throw new SpeakingSessionAccessDeniedException("본인의 세션만 접근할 수 있습니다.");
        }

        return session;
    }
}
