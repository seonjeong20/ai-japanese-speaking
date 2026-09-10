package com.aijapanese.speaking.conversation.dto;

public record ConversationTurnResponse(
        MessageResponse userMessage,
        MessageResponse aiMessage
) {
}
