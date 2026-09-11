package com.aijapanese.speaking.interview.ai;

import com.aijapanese.speaking.ai.AiClient;
import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.interview.entity.EvaluationCriterion;
import com.aijapanese.speaking.interview.entity.InterviewSetting;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 면접 전체 종료 후 종합 Feedback을 생성하는 Domain AI Service.
 * 질문·답변별 평가/Coaching은 이미 완료된 상태이며, 이 Service는 그 결과를 종합할 뿐
 * 점수를 다시 계산하거나 답변을 재평가하지 않는다(점수는 Backend가 계산해 참고 정보로 전달한다).
 */
@Service
public class InterviewFinalFeedbackAiService {

    private final AiClient aiClient;
    private final BeanOutputConverter<InterviewFinalFeedbackAiResult> converter =
            new BeanOutputConverter<>(InterviewFinalFeedbackAiResult.class);

    public InterviewFinalFeedbackAiService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public InterviewFinalFeedbackAiResult summarize(
            InterviewSetting setting,
            List<InterviewAnsweredQuestionSummary> answers,
            BigDecimal overallScore,
            Map<EvaluationCriterion, BigDecimal> criterionAverages
    ) {
        List<Message> messages = List.of(
                new SystemMessage(buildSystemPrompt()),
                new UserMessage(buildUserPrompt(setting, answers, overallScore, criterionAverages) + "\n\n" + converter.getFormat())
        );

        try {
            InterviewFinalFeedbackAiResult result = converter.convert(aiClient.chat(new Prompt(messages)));
            validate(result);
            return result;
        } catch (AiServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiServiceException("면접 종합 Feedback 결과를 처리할 수 없습니다.", e);
        }
    }

    private String buildSystemPrompt() {
        return """
                당신은 일본 취업 면접 전체를 종합하는 코치입니다.
                각 질문에 대한 답변 평가와 Coaching은 이미 완료되었습니다.
                당신의 역할은 그 결과를 종합해 면접 전체에 대한 요약, 강점, 개선점을 작성하는 것입니다.

                점수를 다시 계산하거나 새로 평가하지 마세요. 제공된 요약과 점수는 참고용입니다.
                사용자가 답변에서 실제로 말하지 않은 경험, 프로젝트, 회사, 기술, 수치, 성과를 만들거나 추정하지 마세요.
                strengths와 improvements는 개별 질문 평가를 그대로 나열하지 말고, 면접 전체에서 반복적으로 나타난
                경향을 종합해 각각 최대 5개까지만 작성하세요.
                summary, strengths, improvements는 모두 한국어로 작성하세요.
                """;
    }

    private String buildUserPrompt(
            InterviewSetting setting,
            List<InterviewAnsweredQuestionSummary> answers,
            BigDecimal overallScore,
            Map<EvaluationCriterion, BigDecimal> criterionAverages
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("지원 직무: ").append(setting.getJobRole()).append("\n");
        prompt.append("면접 유형: ").append(setting.getInterviewType() == null ? "미지정" : setting.getInterviewType()).append("\n");
        prompt.append("난이도: ").append(setting.getDifficulty()).append("\n");
        prompt.append("답변한 질문 수: ").append(answers.size()).append("\n\n");

        prompt.append("전체 평균 점수(Backend 계산, 참고용): ").append(overallScore).append("\n");
        prompt.append("항목별 평균 점수(Backend 계산, 참고용):\n");
        criterionAverages.forEach((criterion, average) ->
                prompt.append("- ").append(criterion).append(": ").append(average).append("\n"));
        prompt.append("\n질문·답변 및 개별 평가 요약:\n");

        for (InterviewAnsweredQuestionSummary answer : answers) {
            prompt.append("Q").append(answer.sequenceNo()).append(" (").append(answer.questionKind()).append("): ")
                    .append(answer.questionText()).append("\n");
            prompt.append("답변: ").append(answer.answerText()).append("\n");
            prompt.append("평가 요약: ").append(answer.evaluationSummary())
                    .append(" (점수: ").append(answer.overallScore()).append(")\n");
            prompt.append("강점: ").append(answer.strengths()).append("\n");
            prompt.append("약점: ").append(answer.weaknesses()).append("\n\n");
        }

        prompt.append("위 내용을 종합해 면접 전체에 대한 summary, strengths, improvements를 생성하세요.");
        return prompt.toString();
    }

    private void validate(InterviewFinalFeedbackAiResult result) {
        if (result == null
                || !hasText(result.summary())
                || result.strengths() == null
                || result.improvements() == null) {
            throw new AiServiceException("면접 종합 Feedback 결과가 비어 있습니다.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
