package com.aijapanese.speaking.history.dto;

import com.aijapanese.speaking.conversation.dto.ConversationFeedbackResponse;
import com.aijapanese.speaking.interview.dto.InterviewFeedbackResponse;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * History 상세 응답. sessionType에 따라 conversationFeedback 또는 interviewFeedback 중
 * 하나만 채워진다. settings는 세션 시작 당시 설정을 모드별로 담은 값이다(docs/openapi
 * HistoryDetail.settings: additionalProperties 스키마와 맞춤).
 */
public record HistoryDetailResponse(
        Long sessionId,
        SessionType sessionType,
        SessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer durationSeconds,
        Map<String, Object> settings,
        List<HistoryMessageResponse> transcript,
        ConversationFeedbackResponse conversationFeedback,
        InterviewFeedbackResponse interviewFeedback
) {
}
