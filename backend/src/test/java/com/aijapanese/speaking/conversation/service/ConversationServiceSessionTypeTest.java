package com.aijapanese.speaking.conversation.service;

import com.aijapanese.speaking.conversation.ai.ConversationAiService;
import com.aijapanese.speaking.conversation.ai.ConversationFeedbackAiService;
import com.aijapanese.speaking.conversation.dto.SessionCompletionResponse;
import com.aijapanese.speaking.conversation.repository.ConversationCorrectionRepository;
import com.aijapanese.speaking.conversation.repository.ConversationFeedbackRepository;
import com.aijapanese.speaking.conversation.repository.ConversationSettingRepository;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.SpeakingSession;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotFoundException;
import com.aijapanese.speaking.speaking.repository.SpeakingMessageRepository;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceSessionTypeTest {

    @Mock private SpeakingSessionRepository speakingSessionRepository;
    @Mock private ConversationSettingRepository conversationSettingRepository;
    @Mock private SpeakingMessageRepository speakingMessageRepository;
    @Mock private ConversationFeedbackRepository conversationFeedbackRepository;
    @Mock private ConversationCorrectionRepository conversationCorrectionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConversationAiService conversationAiService;
    @Mock private ConversationFeedbackAiService conversationFeedbackAiService;
    @Mock private User user;

    @InjectMocks private ConversationService conversationService;

    @Test
    void completeConversation_completesConversationSession() {
        SpeakingSession session = new SpeakingSession(
                user, SessionType.CONVERSATION, SessionStatus.IN_PROGRESS,
                LocalDateTime.now(), LocalDateTime.now()
        );
        ReflectionTestUtils.setField(session, "id", 2L);
        when(user.getId()).thenReturn(1L);
        when(speakingSessionRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(session));
        when(speakingMessageRepository.findBySession_IdOrderBySequenceNoAsc(2L)).thenReturn(List.of());

        SessionCompletionResponse response = conversationService.completeConversation(2L, 1L);

        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getEndedAt()).isNotNull();
        assertThat(session.getDurationSeconds()).isGreaterThanOrEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"complete", "abort", "opening", "textTurn", "audioTurn", "recordUserMessage", "feedback"})
    void existingSessionOperations_rejectInterviewBeforeAnySideEffects(String operation) {
        LocalDateTime startedAt = LocalDateTime.now();
        SpeakingSession interview = new SpeakingSession(
                user, SessionType.INTERVIEW, SessionStatus.IN_PROGRESS, startedAt, startedAt
        );
        when(user.getId()).thenReturn(1L);
        if (operation.equals("feedback")) {
            when(speakingSessionRepository.findById(2L)).thenReturn(Optional.of(interview));
        } else {
            when(speakingSessionRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(interview));
        }

        assertThrows(SpeakingSessionNotFoundException.class, () -> {
            switch (operation) {
                case "complete" -> conversationService.completeConversation(2L, 1L);
                case "abort" -> conversationService.abortConversation(2L, 1L);
                case "opening" -> conversationService.openConversation(2L, 1L);
                case "textTurn" -> conversationService.processTextTurn(2L, 1L, "こんにちは");
                case "audioTurn" -> conversationService.processAudioTurn(
                        2L, 1L, new ByteArrayResource(new byte[]{1, 2, 3})
                );
                case "recordUserMessage" -> conversationService.recordUserMessage(2L, 1L, "こんにちは");
                case "feedback" -> conversationService.getFeedback(2L, 1L);
                default -> throw new IllegalArgumentException(operation);
            }
        });

        assertThat(interview.getSessionType()).isEqualTo(SessionType.INTERVIEW);
        assertThat(interview.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(interview.getStartedAt()).isEqualTo(startedAt);
        assertThat(interview.getEndedAt()).isNull();
        assertThat(interview.getDurationSeconds()).isNull();
        verifyNoInteractions(
                conversationSettingRepository, speakingMessageRepository,
                conversationFeedbackRepository, conversationCorrectionRepository,
                userRepository, conversationAiService, conversationFeedbackAiService
        );
    }
}
