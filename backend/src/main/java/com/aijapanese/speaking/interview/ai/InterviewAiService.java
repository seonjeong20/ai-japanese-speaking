package com.aijapanese.speaking.interview.ai;

import com.aijapanese.speaking.ai.AiClient;
import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.interview.entity.InterviewQuestion;
import com.aijapanese.speaking.interview.entity.InterviewSetting;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Interview(면접)의 첫 질문 생성을 담당하는 Domain AI Service.
 * Prompt 구성 책임은 여기에 두고, 실제 OpenAI 호출은 {@link AiClient}에 위임한다.
 * Conversation과는 별도의 프롬프트/파싱 로직을 가진다(ConversationAiService 재사용 없음).
 */
@Service
public class InterviewAiService {

    private final AiClient aiClient;
    private final BeanOutputConverter<InterviewFirstQuestionResult> firstQuestionConverter =
            new BeanOutputConverter<>(InterviewFirstQuestionResult.class);

    public InterviewAiService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public InterviewFirstQuestionResult generateFirstQuestion(InterviewSetting setting) {
        return generateQuestion(buildFirstQuestionPrompt(setting));
    }

    public InterviewFirstQuestionResult generateNextInitialQuestion(
            InterviewSetting setting,
            List<InterviewQuestion> previousQuestions
    ) {
        return generateQuestion(buildNextInitialQuestionPrompt(setting, previousQuestions));
    }

    private InterviewFirstQuestionResult generateQuestion(String promptText) {
        try {
            List<Message> messages = List.of(
                    new UserMessage(promptText + "\n\n" + firstQuestionConverter.getFormat())
            );
            InterviewFirstQuestionResult result = firstQuestionConverter.convert(aiClient.chat(new Prompt(messages)));
            if (result == null || !hasText(result.questionText())) {
                throw new AiServiceException("면접 질문 생성 결과가 비어 있습니다.");
            }
            return result;
        } catch (AiServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiServiceException("면접 질문 생성 결과를 처리할 수 없습니다.", e);
        }
    }

    private String buildFirstQuestionPrompt(InterviewSetting setting) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 일본 기업의 면접관입니다. 지원자와의 실제 면접 도입부에 어울리는 첫 질문을 하나만 생성하세요.\n");
        prompt.append("이 단계에서는 평가, 점수, 피드백, 꼬리질문을 절대 생성하지 마세요. 오직 면접 첫 질문 하나만 만드세요.\n");
        prompt.append("지원 직무: ").append(setting.getJobRole()).append("\n");

        if (hasText(setting.getInterviewType())) {
            prompt.append("면접 유형: ").append(setting.getInterviewType()).append("\n");
        }
        if (hasText(setting.getAdditionalRequest())) {
            prompt.append("추가 요청 사항: ").append(setting.getAdditionalRequest()).append("\n");
        }

        prompt.append("지원자의 일본어 난이도: ").append(setting.getDifficulty()).append("\n");
        switch (setting.getDifficulty()) {
            case BEGINNER -> prompt.append("짧고 쉬운 문장, 기초 어휘로 질문하세요.\n");
            case INTERMEDIATE -> prompt.append("일상적인 비즈니스 어휘와 자연스러운 길이의 문장으로 질문하세요.\n");
            case ADVANCED -> prompt.append("자연스럽고 격식 있는 비즈니스 일본어로 질문하세요.\n");
        }

        prompt.append("질문은 반드시 일본어로만 작성하세요. 자기소개나 지원 동기를 묻는 등 실제 면접의 시작 질문으로 자연스러워야 합니다.\n");
        prompt.append("질문은 한 문장(또는 짧은 두 문장 이내)으로 작성하세요.");

        return prompt.toString();
    }

    private String buildNextInitialQuestionPrompt(
            InterviewSetting setting,
            List<InterviewQuestion> previousQuestions
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 일본 기업의 면접관입니다. 다음 기본 면접 질문(INITIAL)을 정확히 하나 생성하세요.\n");
        prompt.append("평가, Coaching, Follow-up, 완료 판단은 생성하지 마세요.\n");
        prompt.append("지원 직무: ").append(setting.getJobRole()).append("\n");
        prompt.append("면접 유형: ").append(hasText(setting.getInterviewType()) ? setting.getInterviewType() : "미지정").append("\n");
        prompt.append("난이도: ").append(setting.getDifficulty()).append("\n");
        prompt.append("이미 출제된 질문과 의미가 중복되지 않는 새로운 주제를 선택하세요.\n");
        prompt.append("이미 출제된 질문:\n");
        previousQuestions.forEach(question -> prompt.append("- ").append(question.getQuestionText()).append("\n"));
        prompt.append("질문은 반드시 일본어로만, 한 문장 또는 짧은 두 문장 이내로 작성하세요.");
        return prompt.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
