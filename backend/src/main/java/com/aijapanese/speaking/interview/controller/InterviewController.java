package com.aijapanese.speaking.interview.controller;

import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.conversation.dto.SessionCompletionResponse;
import com.aijapanese.speaking.interview.dto.InterviewAnswerRequest;
import com.aijapanese.speaking.interview.dto.InterviewAnswerResponse;
import com.aijapanese.speaking.interview.dto.InterviewFeedbackResponse;
import com.aijapanese.speaking.interview.dto.InterviewStartRequest;
import com.aijapanese.speaking.interview.dto.InterviewStartResponse;
import com.aijapanese.speaking.interview.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    public ResponseEntity<InterviewStartResponse> start(
            @Valid @RequestBody InterviewStartRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        InterviewStartResponse response = interviewService.startInterview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    public ResponseEntity<InterviewAnswerResponse> submitTextAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody InterviewAnswerRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(interviewService.submitTextAnswer(
                sessionId, questionId, userId, request.answerText()
        ));
    }

    @PostMapping(
            value = "/{sessionId}/questions/{questionId}/answer/audio",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<InterviewAnswerResponse> submitAudioAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @RequestParam("audio") MultipartFile audio,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(interviewService.submitAudioAnswer(
                sessionId, questionId, userId, toResource(audio)
        ));
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<SessionCompletionResponse> complete(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(interviewService.completeInterview(sessionId, userId));
    }

    @GetMapping("/{sessionId}/feedback")
    public ResponseEntity<InterviewFeedbackResponse> getFeedback(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(interviewService.getFeedback(sessionId, userId));
    }

    private Resource toResource(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String extension = resolveExtension(file.getContentType());
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

    private String resolveExtension(String contentType) {
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
