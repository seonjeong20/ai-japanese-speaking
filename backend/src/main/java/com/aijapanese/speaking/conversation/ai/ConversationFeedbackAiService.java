package com.aijapanese.speaking.conversation.ai;

import com.aijapanese.speaking.ai.AiClient;
import com.aijapanese.speaking.conversation.entity.ConversationSetting;
import com.aijapanese.speaking.speaking.entity.Speaker;
import com.aijapanese.speaking.speaking.entity.SpeakingMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Conversation 완료 시 전체 Transcript를 기반으로 Feedback을 생성하는 Domain AI Service.
 * 세션당 최대 1회만 호출된다(호출 시점 제어는 ConversationService의 책임).
 */
@Service
public class ConversationFeedbackAiService {

    private final AiClient aiClient;
    private final BeanOutputConverter<ConversationFeedbackAiResult> feedbackConverter =
            new BeanOutputConverter<>(ConversationFeedbackAiResult.class);

    public ConversationFeedbackAiService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public ConversationFeedbackAiResult generateFeedback(ConversationSetting setting, List<SpeakingMessage> transcript) {
        List<Message> messages = List.of(
                new SystemMessage(buildSystemPrompt()),
                new UserMessage(buildTranscriptPrompt(setting, transcript) + "\n\n" + feedbackConverter.getFormat())
        );

        String raw = aiClient.chat(new Prompt(messages));
        return feedbackConverter.convert(raw);
    }

    private String buildSystemPrompt() {
        return """
                당신은 일본어 회화 학습 Feedback을 작성하는 일본어 교육 전문가입니다.
                사용자의 답변 내용의 논리성이나 적절성은 평가하지 않습니다. 오직 일본어 표현 품질(자연스러움, 문법, 어휘)에만 집중하세요.
                숫자 점수(예: 80점, 90/100)는 절대 생성하지 마세요. 모든 평가는 텍스트로만 작성하세요.
                corrections는 사용자(USER) 발화 중 개선하면 좋을 표현을 우선순위 순으로 선택하고, 각 항목의 relatedUserMessageSequenceNo에는
                해당 발화의 sequenceNo를 정확히 넣으세요. 특정 발화와 연결하기 어렵다면 null로 두세요.
                각 correction의 reading에는 suggestedExpression 전체를 한자를 몰라도 읽을 수 있도록 히라가나 중심으로 작성하세요.
                각 correction의 koreanTranslation에는 suggestedExpression을 자연스러운 한국어로 번역해 작성하세요.
                모든 코멘트와 설명, koreanTranslation은 한국어로 작성하고, originalExpression/suggestedExpression/reading은 일본어로 작성하세요.
                """;
    }

    private String buildTranscriptPrompt(ConversationSetting setting, List<SpeakingMessage> transcript) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 사용자가 방금 마친 일본어 일반 회화 전체 기록입니다.\n");

        if (setting != null) {
            prompt.append("난이도: ").append(setting.getDifficulty()).append("\n");
            if (setting.getSituation() != null) {
                prompt.append("상황: ").append(setting.getSituation()).append("\n");
            }
        }

        prompt.append("대화 기록:\n");
        for (SpeakingMessage message : transcript) {
            String speaker = message.getSpeaker() == Speaker.USER ? "USER" : "AI";
            prompt.append(String.format("[seq=%d][%s] %s%n", message.getSequenceNo(), speaker, message.getContent()));
        }

        prompt.append("\n이 대화를 바탕으로 구조화된 일본어 학습 Feedback을 생성하세요.");
        return prompt.toString();
    }
}
