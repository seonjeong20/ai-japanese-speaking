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

    private static final String OPENING_INSTRUCTION =
            "지금부터 대화를 시작합니다. 사용자는 아직 아무 말도 하지 않았습니다. "
                    + "위에서 설정된 상황과 당신의 역할, 성격에 맞게 상대방으로서 자연스럽게 먼저 말을 건네며 대화를 시작하세요.";

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

        return callForReply(messages);
    }

    /**
     * 사용자 발화 없이, 설정된 상황/역할/성격에 맞춰 상대방으로서 대화를 먼저 시작하는 첫 발화를 생성한다.
     * generateReply와 system prompt 구성 및 응답 파싱 로직을 공유하고, 대화 이력 대신
     * "먼저 말을 건네라"는 목적이 분명한 지시만 사용자 메시지로 전달한다는 점만 다르다.
     */
    public ConversationAiReply generateOpeningReply(ConversationSetting setting) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt(setting)));
        messages.add(new UserMessage(OPENING_INSTRUCTION + "\n\n" + replyConverter.getFormat()));

        return callForReply(messages);
    }

    /**
     * 이미 생성되어 저장된 AI 발화(japaneseText)를 한국어로 번역한다.
     * opening이 이미 존재해 재사용할 때, 새로운 opening을 다시 생성하지 않으면서
     * 응답에 필요한 한국어 자막만 다시 만들어내기 위해 사용한다.
     */
    public String translateToKorean(String japaneseText) {
        String prompt = "다음 일본어 문장을 자연스러운 한국어 한 문장으로 번역하세요. 번역문만 출력하고 다른 말은 덧붙이지 마세요.\n\n"
                + japaneseText;
        return aiClient.chat(new Prompt(List.of(new UserMessage(prompt)))).strip();
    }

    private ConversationAiReply callForReply(List<Message> messages) {
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
