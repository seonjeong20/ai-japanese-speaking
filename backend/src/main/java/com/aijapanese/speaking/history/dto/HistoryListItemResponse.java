package com.aijapanese.speaking.history.dto;

import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * My History 목록에 표시되는 세션 1건 요약.
 * CONVERSATION은 숫자 점수를 쓰지 않으므로(ai-design.md 7.3) overallScore는 null이다.
 */
public record HistoryListItemResponse(
        Long sessionId,
        SessionType sessionType,
        SessionStatus status,
        String title,
        LocalDateTime startedAt,
        Integer durationSeconds,
        BigDecimal overallScore
) {
}
