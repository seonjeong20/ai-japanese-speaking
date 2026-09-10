package com.aijapanese.speaking.conversation.service;

import com.aijapanese.speaking.conversation.ai.ConversationAiReply;
import com.aijapanese.speaking.conversation.ai.ConversationAiService;
import com.aijapanese.speaking.conversation.ai.ConversationFeedbackAiResult;
import com.aijapanese.speaking.conversation.ai.ConversationFeedbackAiService;
import com.aijapanese.speaking.conversation.dto.ConversationFeedbackResponse;
import com.aijapanese.speaking.conversation.dto.ConversationStartRequest;
import com.aijapanese.speaking.conversation.dto.SessionCompletionResponse;
import com.aijapanese.speaking.conversation.entity.CorrectionCategory;
import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.GenerationStatus;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.conversation.exception.ConversationFeedbackNotFoundException;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.speaking.dto.SpeakingSessionResponse;
import com.aijapanese.speaking.speaking.entity.SessionStatus;
import com.aijapanese.speaking.speaking.entity.SessionType;
import com.aijapanese.speaking.speaking.entity.Speaker;
import com.aijapanese.speaking.speaking.entity.SpeakingMessage;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotInProgressException;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ConversationAiService/ConversationFeedbackAiService는 항상 @MockitoBean으로 대체한다.
 * 이 테스트는 실제 OpenAI API를 절대 호출하지 않는다 (AiClient까지 내려가지 않음).
 */
@SpringBootTest
@Transactional
class ConversationServiceIntegrationTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockitoBean
    private ConversationAiService conversationAiService;

    @MockitoBean
    private ConversationFeedbackAiService conversationFeedbackAiService;

    private Long createLearner(String email) {
        Organization organization = organizationRepository.save(
                new Organization("Test Org " + email, OrganizationStatus.ACTIVE, LocalDateTime.now())
        );
        User user = new User(
                organization,
                "Test Learner",
                email,
                "hash",
                null,
                UserRole.LEARNER,
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
        return userRepository.save(user).getId();
    }

    @Test
    void startConversation_createsSessionAndSetting() {
        Long userId = createLearner("conv-test-1@example.com");

        ConversationStartRequest request = new ConversationStartRequest(
                "CAFE", "친구", "밝고 친근함", "테스트 상황", Difficulty.INTERMEDIATE, SubtitleMode.JAPANESE_KOREAN
        );

        SpeakingSessionResponse response = conversationService.startConversation(userId, request);

        assertThat(response.sessionId()).isNotNull();
        assertThat(response.sessionType()).isEqualTo(SessionType.CONVERSATION);
        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(response.startedAt()).isNotNull();
    }

    @Test
    void recordUserMessage_persistsMessageWithIncrementingSequence() {
        Long userId = createLearner("conv-test-2@example.com");
        ConversationStartRequest request = new ConversationStartRequest(
                null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF
        );
        SpeakingSessionResponse session = conversationService.startConversation(userId, request);

        SpeakingMessage first = conversationService.recordUserMessage(session.sessionId(), userId, "こんにちは");
        SpeakingMessage second = conversationService.recordUserMessage(session.sessionId(), userId, "元気ですか");

        assertThat(first.getSequenceNo()).isEqualTo(1);
        assertThat(second.getSequenceNo()).isEqualTo(2);
        assertThat(first.getSpeaker()).isEqualTo(Speaker.USER);
        assertThat(first.getMessageType()).isEqualTo("CONVERSATION");
        assertThat(first.getContent()).isEqualTo("こんにちは");
    }

    @Test
    void recordUserMessage_rejectsOtherUsersSession() {
        Long ownerId = createLearner("conv-test-3-owner@example.com");
        Long otherId = createLearner("conv-test-3-other@example.com");
        SpeakingSessionResponse session = conversationService.startConversation(
                ownerId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );

        assertThrows(SpeakingSessionAccessDeniedException.class,
                () -> conversationService.recordUserMessage(session.sessionId(), otherId, "hi"));
    }

    @Test
    void processTextTurn_savesUserAndAiMessagesUsingMockedAiService() {
        Long userId = createLearner("conv-test-turn@example.com");
        SpeakingSessionResponse session = conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.JAPANESE_KOREAN)
        );

        when(conversationAiService.generateReply(any(), any(), anyString()))
                .thenReturn(new ConversationAiReply("元気ですよ、ありがとう。", "잘 지내요, 고마워요."));

        ConversationService.TurnResult result = conversationService.processTextTurn(
                session.sessionId(), userId, "元気ですか?"
        );

        assertThat(result.userMessage().getContent()).isEqualTo("元気ですか?");
        assertThat(result.userMessage().getSequenceNo()).isEqualTo(1);
        assertThat(result.aiMessage().getContent()).isEqualTo("元気ですよ、ありがとう。");
        assertThat(result.aiMessage().getSequenceNo()).isEqualTo(2);
        assertThat(result.aiMessage().getSpeaker()).isEqualTo(Speaker.AI);
        assertThat(result.aiKoreanSubtitle()).isEqualTo("잘 지내요, 고마워요.");
    }

    @Test
    void completeConversation_withNoMessages_marksFeedbackFailedWithoutCallingAi() {
        Long userId = createLearner("conv-test-4@example.com");
        SpeakingSessionResponse session = conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );

        SessionCompletionResponse completed = conversationService.completeConversation(session.sessionId(), userId);

        assertThat(completed.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.endedAt()).isNotNull();
        assertThat(completed.durationSeconds()).isGreaterThanOrEqualTo(0);
        assertThat(completed.feedbackGenerationStatus()).isEqualTo(GenerationStatus.FAILED);
        org.mockito.Mockito.verifyNoInteractions(conversationFeedbackAiService);
    }

    @Test
    void completeConversation_generatesAndPersistsFeedback_whenAiSucceeds() {
        Long userId = createLearner("conv-test-6@example.com");
        SpeakingSessionResponse session = conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );
        conversationService.recordUserMessage(session.sessionId(), userId, "こんにちは、元気ですか?");

        ConversationFeedbackAiResult aiResult = new ConversationFeedbackAiResult(
                "전반적으로 잘했어요.", "자연스러웠습니다.", "문법도 정확했습니다.", "어휘도 다양했습니다.",
                List.of("인사를 잘했어요."), "다음엔 존댓말도 연습해보세요.",
                List.of(new ConversationFeedbackAiResult.Correction(
                        CorrectionCategory.NATURALNESS, "こんにちは", "こんにちは!",
                        "こんにちは!", "안녕하세요!", "느낌표를 붙이면 더 자연스러워요.", 1
                ))
        );
        when(conversationFeedbackAiService.generateFeedback(any(), any())).thenReturn(aiResult);

        SessionCompletionResponse completed = conversationService.completeConversation(session.sessionId(), userId);
        assertThat(completed.feedbackGenerationStatus()).isEqualTo(GenerationStatus.COMPLETED);

        ConversationFeedbackResponse feedback = conversationService.getFeedback(session.sessionId(), userId);
        assertThat(feedback.generationStatus()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(feedback.summary()).isEqualTo("전반적으로 잘했어요.");
        assertThat(feedback.strengths()).containsExactly("인사를 잘했어요.");
        assertThat(feedback.corrections()).hasSize(1);
        assertThat(feedback.corrections().get(0).category()).isEqualTo(CorrectionCategory.NATURALNESS);
        assertThat(feedback.corrections().get(0).messageId()).isNotNull();
        assertThat(feedback.corrections().get(0).reading()).isEqualTo("こんにちは!");
        assertThat(feedback.corrections().get(0).koreanTranslation()).isEqualTo("안녕하세요!");
    }

    @Test
    void completeConversation_marksFeedbackFailed_whenAiThrows() {
        Long userId = createLearner("conv-test-7@example.com");
        SpeakingSessionResponse session = conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );
        conversationService.recordUserMessage(session.sessionId(), userId, "こんにちは");

        when(conversationFeedbackAiService.generateFeedback(any(), any()))
                .thenThrow(new RuntimeException("simulated AI failure"));

        SessionCompletionResponse completed = conversationService.completeConversation(session.sessionId(), userId);

        assertThat(completed.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.feedbackGenerationStatus()).isEqualTo(GenerationStatus.FAILED);
    }

    @Test
    void getFeedback_throwsWhenNotYetGenerated() {
        Long userId = createLearner("conv-test-8@example.com");
        SpeakingSessionResponse session = conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );

        assertThrows(ConversationFeedbackNotFoundException.class,
                () -> conversationService.getFeedback(session.sessionId(), userId));
    }

    @Test
    void completeConversation_rejectsAlreadyCompletedSession() {
        Long userId = createLearner("conv-test-5@example.com");
        SpeakingSessionResponse session = conversationService.startConversation(
                userId, new ConversationStartRequest(null, null, null, null, Difficulty.BEGINNER, SubtitleMode.OFF)
        );
        conversationService.completeConversation(session.sessionId(), userId);

        assertThrows(SpeakingSessionNotInProgressException.class,
                () -> conversationService.completeConversation(session.sessionId(), userId));
    }
}
