package com.aijapanese.speaking.conversation.dto;

/**
 * 브라우저 음성 녹음을 위한 확장 turn 응답.
 * 문서화된 ConversationTurnResponse(userMessage/aiMessage) 계약은 그대로 유지하면서,
 * 실제 음성 재생에 필요한 AI 오디오와(자막 모드에 따른) 한국어 자막을 추가로 담는다.
 * 기존 텍스트 기반 turns 계약을 변경하지 않는 별도의 응답 형태다.
 */
public record ConversationAudioTurnResponse(
        MessageResponse userMessage,
        MessageResponse aiMessage,
        String aiMessageKoreanSubtitle,
        String aiAudioBase64,
        String aiAudioMimeType
) {
}
