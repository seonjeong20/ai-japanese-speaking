package com.aijapanese.speaking.conversation.ai;

import com.aijapanese.speaking.ai.AiClient;
import com.aijapanese.speaking.conversation.entity.ConversationSetting;
import com.aijapanese.speaking.speaking.entity.Speaker;
import com.aijapanese.speaking.speaking.entity.SpeakingMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversation(일반 회화)의 STT / LLM 응답 생성 / TTS를 담당하는 Domain AI Service.
 * Prompt 구성 책임은 여기에 두고, 실제 OpenAI 호출은 {@link AiClient}에 위임한다.
 */
@Service
public class ConversationAiService {

    private final AiClient aiClient;
    private final BeanOutputConverter<ConversationAiReply> replyConverter =
            new BeanOutputConverter<>(ConversationAiReply.class);

    public ConversationAiService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public String transcribe(Resource audioResource) {
        return aiClient.transcribe(audioResource);
    }

    public byte[] synthesizeSpeech(String japaneseText) {
        return aiClient.synthesizeSpeech(japaneseText);
    }

    public ConversationAiReply generateReply(
            ConversationSetting setting,
            List<SpeakingMessage> recentHistory,
            String userMessage
    ) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt(setting)));

        for (SpeakingMessage message : recentHistory) {
            if (message.getSpeaker() == Speaker.USER) {
                messages.add(new UserMessage(message.getContent()));
            } else {
                messages.add(new AssistantMessage(message.getContent()));
            }
        }

        messages.add(new UserMessage(userMessage + "\n\n" + replyConverter.getFormat()));

        String raw = aiClient.chat(new Prompt(messages));
        return replyConverter.convert(raw);
    }

    private String buildSystemPrompt(ConversationSetting setting) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 사용자의 일본어 회화 연습 상대입니다. 평가자가 아니라 대화 상대로서 자연스럽게 일본어 대화를 이어가세요.\n");
        prompt.append("항상 일본어로만 응답하세요. 문법이나 표현을 지적하거나 채점하지 마세요. 그런 평가는 이 대화가 끝난 뒤 별도로 이루어집니다.\n");

        if (hasText(setting.getSituation())) {
            prompt.append("상황: ").append(setting.getSituation()).append("\n");
        }
        if (hasText(setting.getPartnerRole())) {
            prompt.append("당신의 역할: ").append(setting.getPartnerRole()).append("\n");
        }
        if (hasText(setting.getPartnerPersonality())) {
            prompt.append("당신의 성격: ").append(setting.getPartnerPersonality()).append("\n");
        }
        if (hasText(setting.getSituationDescription())) {
            prompt.append("추가 상황 설명: ").append(setting.getSituationDescription()).append("\n");
        }

        prompt.append("사용자의 일본어 난이도: ").append(setting.getDifficulty()).append("\n");
        switch (setting.getDifficulty()) {
            case BEGINNER -> prompt.append("짧고 쉬운 문장, 기초 어휘를 사용해 천천히 대화하세요.\n");
            case INTERMEDIATE -> prompt.append("일상적인 어휘와 자연스러운 길이의 문장을 사용하세요.\n");
            case ADVANCED -> prompt.append("자연스럽고 다양한 표현, 관용구를 포함해 실제 원어민처럼 대화하세요.\n");
        }

        prompt.append("응답은 1~3문장 이내로 짧게 유지하세요.\n");
        prompt.append("koreanTranslation 필드에는 japaneseText에 대한 자연스러운 한국어 번역을 함께 제공하세요.");

        return prompt.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
