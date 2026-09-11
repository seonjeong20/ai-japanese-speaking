package com.aijapanese.speaking.interview.ai;

import com.aijapanese.speaking.ai.AiClient;
import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.interview.entity.InterviewQuestion;
import com.aijapanese.speaking.interview.entity.InterviewSetting;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewAnswerCoachingAiService {

    private final AiClient aiClient;
    private final BeanOutputConverter<InterviewAnswerCoachingAiResult> converter =
            new BeanOutputConverter<>(InterviewAnswerCoachingAiResult.class);

    public InterviewAnswerCoachingAiService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public InterviewAnswerCoachingAiResult coach(
            InterviewSetting setting,
            InterviewQuestion question,
            String answerText,
            InterviewAnswerEvaluationAiResult evaluation,
            boolean followUpAllowed
    ) {
        List<Message> messages = List.of(
                new SystemMessage(buildSystemPrompt()),
                new UserMessage(buildCoachingPrompt(
                        setting, question, answerText, evaluation, followUpAllowed
                ) + "\n\n" + converter.getFormat())
        );

        try {
            InterviewAnswerCoachingAiResult result = converter.convert(aiClient.chat(new Prompt(messages)));
            validate(result, followUpAllowed);
            return result;
        } catch (AiServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiServiceException("면접 답변 Coaching 결과를 처리할 수 없습니다.", e);
        }
    }

    private String buildSystemPrompt() {
        return """
                당신은 일본 취업 면접 답변 개선을 돕는 코치입니다.
                이미 완료된 평가를 반복하거나 점수를 다시 계산하지 마세요.
                평가 결과를 근거로 답변을 더 명확하고 구체적이며 면접에 적합하게 개선하는 방법만 제시하세요.

                modelAnswer는 사용자의 실제 답변에 존재하는 경험과 사실만 사용해 일본어로 작성하세요.
                사용자가 말하지 않은 경험, 프로젝트, 회사, 기술, 수치, 기간, 성과를 창작하거나 추정하지 마세요.
                구체적인 정보가 필요하지만 원문에 없다면 [具体的な成果], [担当した役割]처럼 명백한 placeholder를 사용하세요.
                coachingSummary, improvementTips, followUpReason은 한국어로 작성하세요.
                followUpQuestion은 일본어 면접 질문 한 개만 작성하세요.
                Follow-up은 현재 답변의 중요한 모호점이나 추가 확인이 실제로 필요할 때만 요청하세요.
                """;
    }

    private String buildCoachingPrompt(
            InterviewSetting setting,
            InterviewQuestion question,
            String answerText,
            InterviewAnswerEvaluationAiResult evaluation,
            boolean followUpAllowed
    ) {
        String scores = evaluation.evaluations().stream()
                .map(item -> "%s: %s (%s)".formatted(
                        item.criterion(),
                        item.applicable() ? item.score() : "N/A",
                        item.comment()
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

        return """
                지원 직무: %s
                면접 유형: %s
                질문 종류: %s
                질문: %s
                사용자 답변: %s

                기존 평가 요약: %s
                강점: %s
                약점: %s
                평가 항목:
                %s

                Backend 정책상 Follow-up 허용 여부: %s
                허용 여부가 false이면 followUpNeeded=false, followUpQuestion=null로 반환하세요.
                followUpNeeded=false이면 followUpQuestion은 null이어야 합니다.
                """.formatted(
                setting.getJobRole(),
                setting.getInterviewType() == null ? "미지정" : setting.getInterviewType(),
                question.getQuestionKind(),
                question.getQuestionText(),
                answerText,
                evaluation.summary(),
                evaluation.strengths(),
                evaluation.weaknesses(),
                scores,
                followUpAllowed
        );
    }

    private void validate(InterviewAnswerCoachingAiResult result, boolean followUpAllowed) {
        if (result == null
                || !hasText(result.coachingSummary())
                || result.improvementTips() == null
                || !hasText(result.modelAnswer())) {
            throw new AiServiceException("면접 답변 Coaching 필수 항목이 비어 있습니다.");
        }
        if (followUpAllowed && result.followUpNeeded()
                && (!hasText(result.followUpReason()) || !hasText(result.followUpQuestion()))) {
            throw new AiServiceException("Follow-up 결과가 올바르지 않습니다.");
        }
        if (followUpAllowed && !result.followUpNeeded() && hasText(result.followUpQuestion())) {
            throw new AiServiceException("불필요한 Follow-up 질문이 반환되었습니다.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
