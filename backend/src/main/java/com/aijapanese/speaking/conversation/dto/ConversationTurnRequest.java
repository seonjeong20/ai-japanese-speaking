package com.aijapanese.speaking.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public record ConversationTurnRequest(
        @NotBlank String userMessage
) {
}
