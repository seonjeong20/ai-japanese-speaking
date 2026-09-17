package com.aijapanese.speaking.conversation.dto;

/**
 * 일반 회화 세션의 AI 선(先)발화(opening) 응답.
 * 사용자 발화가 없는 시점의 응답이므로 ConversationAudioTurnResponse와 달리 userMessage를 갖지 않는다.
 */
public record ConversationOpeningResponse(
        MessageResponse aiMessage,
        String aiMessageKoreanSubtitle,
        String aiAudioBase64,
        String aiAudioMimeType
) {
}
