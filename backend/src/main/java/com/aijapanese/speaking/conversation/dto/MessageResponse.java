package com.aijapanese.speaking.conversation.dto;

import com.aijapanese.speaking.speaking.entity.SpeakingMessage;

public record MessageResponse(
        Long messageId,
        String content
) {

    public static MessageResponse from(SpeakingMessage message) {
        return new MessageResponse(message.getId(), message.getContent());
    }
}
