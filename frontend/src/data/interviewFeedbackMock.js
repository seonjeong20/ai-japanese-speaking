// 면접 Feedback mock 데이터. 실제 LLM 평가가 연결되면 이 구조 그대로
// Backend API 응답으로 교체하면 됩니다.
export const interviewFeedbackMock = {
  title: 'Backend Developer 면접 연습',
  badgeLabel: '면접 완료',
  subtitle: 'AI가 답변 내용을 종합적으로 분석했어요',
  // 전체 평가 + 전체 점수
  overall: {
    score: 82,
    description:
      '전반적으로 침착하고 논리적인 답변이었어요. 기술 경험을 구체적인 사례로 설명한 점이 특히 좋았습니다. 답변을 조금 더 간결하게 구성하면 더 좋은 인상을 줄 수 있어요.',
  },
  metrics: [
    { id: 'questionFit', label: '질문 적합성', score: 88 },
    { id: 'logic', label: '논리성', score: 84 },
    { id: 'specificity', label: '구체성', score: 79 },
    { id: 'delivery', label: '전달력', score: 81 },
    { id: 'jobFit', label: '직무 적합성', score: 86 },
    { id: 'structure', label: '답변 구조', score: 78 },
    { id: 'businessJapanese', label: '비즈니스 일본어', score: 80 },
  ],
  // 강점
  strengths: ['기술 경험을 구체적인 사례와 수치로 설명했어요.', '질문의 의도를 정확히 파악하고 답변했어요.'],
  // 개선점
  improvements: ['답변이 길어질 때 핵심 결론을 먼저 말하는 연습이 필요해요.', '비즈니스 경어 표현을 조금 더 다듬으면 좋아요.'],
  // 코칭
  coaching: '결론(PREP의 Point)을 답변 맨 앞에 두고, 그 다음 이유와 사례를 덧붙이는 구조로 연습해보세요.',
  questionFeedback: [
    {
      id: 1,
      question: 'Q1. 자기소개 및 지원 동기',
      feedback: '핵심 경험을 구체적으로 잘 설명했어요. 다만 지원 동기 부분은 조금 더 회사와 연결지어 말하면 좋아요.',
      improvedAnswer: '結論から申し上げますと、貴社の〇〇事業に強く共感し、これまでの開発経験を活かしたいと考えたためです。',
    },
    {
      id: 2,
      question: 'Q2. 최근 진행한 프로젝트 경험',
      feedback: '기술 스택과 문제 해결 과정을 논리적으로 설명했어요. 결과 수치를 함께 언급하면 더 설득력 있어요.',
      improvedAnswer: 'その結果、処理速度を約30%改善し、チーム全体の開発効率向上にも貢献しました。',
    },
    {
      id: 3,
      question: 'Q3. 팀에서 의견이 대립했을 때 해결 방법',
      feedback: '상황과 본인의 역할을 명확히 구분해 설명한 점이 좋았어요. 결과적으로 팀에 어떤 영향을 줬는지 덧붙이면 더 좋아요.',
      improvedAnswer: '最終的にはチーム全体の合意を得て進めることができ、プロジェクトの遅延も防ぐことができました。',
    },
  ],
}
