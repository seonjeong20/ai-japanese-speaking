package com.aijapanese.speaking.conversation.controller;

import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.conversation.dto.ConversationAudioTurnResponse;
import com.aijapanese.speaking.conversation.dto.ConversationFeedbackResponse;
import com.aijapanese.speaking.conversation.dto.ConversationStartRequest;
import com.aijapanese.speaking.conversation.dto.ConversationTurnRequest;
import com.aijapanese.speaking.conversation.dto.ConversationTurnResponse;
import com.aijapanese.speaking.conversation.dto.MessageResponse;
import com.aijapanese.speaking.conversation.dto.SessionCompletionResponse;
import com.aijapanese.speaking.conversation.service.ConversationService;
import com.aijapanese.speaking.speaking.dto.SpeakingSessionResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<SpeakingSessionResponse> start(
            @Valid @RequestBody ConversationStartRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        SpeakingSessionResponse response = conversationService.startConversation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{sessionId}/turns")
    public ResponseEntity<ConversationTurnResponse> createTurn(
            @PathVariable Long sessionId,
            @Valid @RequestBody ConversationTurnRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        ConversationService.TurnResult result = conversationService.processTextTurn(sessionId, userId, request.userMessage());

        return ResponseEntity.ok(new ConversationTurnResponse(
                MessageResponse.from(result.userMessage()),
                MessageResponse.from(result.aiMessage())
        ));
    }

    /**
     * 브라우저 MediaRecorder 녹음 파일을 받아 STT → Conversation LLM → TTS까지 한 번에 처리하는
     * 음성 전용 turn 엔드포인트. 문서화된 JSON 기반 /turns 계약은 그대로 유지하고,
     * 실제 음성 E2E에 필요한 새 엔드포인트를 추가한 것이다 (완료 보고에 별도 명시).
     */
    @PostMapping(value = "/{sessionId}/turns/audio", consumes = "multipart/form-data")
    public ResponseEntity<ConversationAudioTurnResponse> createAudioTurn(
            @PathVariable Long sessionId,
            @RequestParam("audio") MultipartFile audio,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Resource audioResource = toResource(audio);

        ConversationService.AudioTurnResult result = conversationService.processAudioTurn(sessionId, userId, audioResource);

        return ResponseEntity.ok(new ConversationAudioTurnResponse(
                MessageResponse.from(result.userMessage()),
                MessageResponse.from(result.aiMessage()),
                result.aiKoreanSubtitle(),
                Base64.getEncoder().encodeToString(result.aiAudio()),
                "audio/mpeg"
        ));
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<SessionCompletionResponse> complete(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(conversationService.completeConversation(sessionId, userId));
    }

    @GetMapping("/{sessionId}/feedback")
    public ResponseEntity<ConversationFeedbackResponse> getFeedback(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(conversationService.getFeedback(sessionId, userId));
    }

    private Resource toResource(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String extension = resolveExtension(file);
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return "audio." + extension;
                }
            };
        } catch (IOException e) {
            throw new AiServiceException("업로드된 오디오 파일을 읽을 수 없습니다.", e);
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("webm")) return "webm";
            if (contentType.contains("mp4") || contentType.contains("m4a")) return "mp4";
            if (contentType.contains("wav")) return "wav";
            if (contentType.contains("mpeg") || contentType.contains("mp3")) return "mp3";
            if (contentType.contains("ogg")) return "ogg";
        }
        return "webm";
    }
}
