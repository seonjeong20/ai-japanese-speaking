package com.aijapanese.speaking.history.dto;

import com.aijapanese.speaking.speaking.entity.SpeakingMessage;

import java.time.LocalDateTime;

public record HistoryMessageResponse(
        Long messageId,
        String content,
        int sequenceNo,
        String speaker,
        String messageType,
        LocalDateTime createdAt
) {

    public static HistoryMessageResponse from(SpeakingMessage message) {
        return new HistoryMessageResponse(
                message.getId(),
                message.getContent(),
                message.getSequenceNo(),
                message.getSpeaker().name(),
                message.getMessageType(),
                message.getCreatedAt()
        );
    }
}
