package com.aijapanese.speaking.interview.ai;

import com.aijapanese.speaking.ai.AiClient;
import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.interview.entity.EvaluationCriterion;
import com.aijapanese.speaking.interview.entity.InterviewQuestion;
import com.aijapanese.speaking.interview.entity.InterviewSetting;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InterviewAnswerEvaluationAiService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);

    private final AiClient aiClient;
    private final BeanOutputConverter<InterviewAnswerEvaluationAiResult> converter =
            new BeanOutputConverter<>(InterviewAnswerEvaluationAiResult.class);

    public InterviewAnswerEvaluationAiService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public InterviewAnswerEvaluationAiResult evaluate(
            InterviewSetting setting,
            InterviewQuestion question,
            String answerText
    ) {
        List<Message> messages = List.of(
                new SystemMessage(buildSystemPrompt()),
                new UserMessage(buildEvaluationPrompt(setting, question, answerText) + "\n\n" + converter.getFormat())
        );

        try {
            InterviewAnswerEvaluationAiResult result = converter.convert(aiClient.chat(new Prompt(messages)));
            validate(result);
            return result;
        } catch (AiServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiServiceException("면접 답변 평가 결과를 처리할 수 없습니다.", e);
        }
    }

    private String buildSystemPrompt() {
        return """
                당신은 일본 취업 면접 질문 분석과 답변 평가를 담당하는 평가자입니다.
                하나의 응답에서 질문 의도, 질문 유형, 핵심 역량, STAR 권장 여부를 분석하고 답변을 평가하세요.
                별도의 코칭, 개선 답변, 다음 질문, 면접 완료 판단은 생성하지 마세요.

                반드시 질문과 사용자의 실제 답변만 근거로 평가하세요.
                사용자가 말하지 않은 경험, 프로젝트, 회사, 기술, 수치, 성과를 만들거나 추정하지 마세요.
                답변에 없는 정보를 사실처럼 평가하지 마세요.
                점수와 comment, strengths, weaknesses가 서로 모순되지 않도록 응답 전 일관성을 검수하세요.

                모든 criterion 점수 범위는 0~100입니다.
                QUESTION_RELEVANCE는 질문 의도에 직접 답했는지 평가합니다.
                LOGICAL_THINKING은 주장과 근거의 논리적 연결을 평가합니다.
                SPECIFICITY는 구체적인 설명, 경험, 근거의 수준을 평가합니다.
                COMMUNICATION은 핵심을 명확하고 적절한 길이로 전달했는지 평가합니다.
                BUSINESS_JAPANESE는 문법, 어휘, 자연스러움, 일본 취업 면접에 적합한 말투를 평가합니다.
                STAR_STRUCTURE는 경험과 행동을 묻는 등 STAR가 적합한 질문일 때만 평가합니다.

                일본어가 완벽하지 않아도 의미 전달이 가능하면 문법 오류만으로 전체 답변 품질을 과도하게 낮추지 마세요.
                일본어 오류는 BUSINESS_JAPANESE에서 별도로 평가하므로 다른 criterion에서 중복 감점하지 마세요.
                여섯 criterion을 각각 정확히 한 번 반환하세요.
                starRecommended=false이면 STAR_STRUCTURE는 applicable=false, score=null로 반환하세요.
                그 외 다섯 criterion은 applicable=true와 0~100 score를 반환하세요.
                summary, strengths, weaknesses, criterion comment는 한국어로 작성하세요.
                """;
    }

    private String buildEvaluationPrompt(
            InterviewSetting setting,
            InterviewQuestion question,
            String answerText
    ) {
        return """
                지원 직무: %s
                면접 유형: %s
                난이도: %s
                면접 질문(일본어): %s
                지원자 답변(STT 일본어): %s

                이 질문과 답변을 분석하고 구조화된 평가 결과를 반환하세요.
                """.formatted(
                setting.getJobRole(),
                setting.getInterviewType() == null ? "미지정" : setting.getInterviewType(),
                setting.getDifficulty(),
                question.getQuestionText(),
                answerText
        );
    }

    private void validate(InterviewAnswerEvaluationAiResult result) {
        if (result == null || result.evaluations() == null) {
            throw new AiServiceException("면접 답변 평가 결과가 비어 있습니다.");
        }
        if (!hasText(result.questionIntent())
                || !hasText(result.questionType())
                || result.coreCompetencies() == null
                || !hasText(result.summary())
                || result.strengths() == null
                || result.weaknesses() == null) {
            throw new AiServiceException("면접 답변 평가 필수 항목이 비어 있습니다.");
        }

        Set<EvaluationCriterion> criteria = result.evaluations().stream()
                .map(InterviewAnswerEvaluationAiResult.CriterionEvaluation::criterion)
                .collect(Collectors.toSet());
        if (result.evaluations().size() != EvaluationCriterion.values().length
                || !criteria.equals(EnumSet.allOf(EvaluationCriterion.class))) {
            throw new AiServiceException("면접 답변 평가 항목이 올바르지 않습니다.");
        }

        for (InterviewAnswerEvaluationAiResult.CriterionEvaluation evaluation : result.evaluations()) {
            boolean starNotApplicable = evaluation.criterion() == EvaluationCriterion.STAR_STRUCTURE
                    && !result.starRecommended();
            if (starNotApplicable) {
                if (evaluation.applicable() || evaluation.score() != null) {
                    throw new AiServiceException("STAR 미적용 평가 결과가 올바르지 않습니다.");
                }
                continue;
            }

            if (!evaluation.applicable() || evaluation.score() == null
                    || evaluation.score().compareTo(MIN_SCORE) < 0
                    || evaluation.score().compareTo(MAX_SCORE) > 0) {
                throw new AiServiceException("면접 답변 평가 점수 범위가 올바르지 않습니다.");
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
