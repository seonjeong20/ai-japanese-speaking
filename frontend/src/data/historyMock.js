// My History mock 데이터. 실제 Backend/DB가 연결되면 이 배열을
// API 응답(예: GET /api/history)으로 교체하면 됩니다.
//
// type: "conversation" | "interview"
// feedback은 각 모드의 Feedback 화면과 동일한 정보 구조를 사용합니다.
// - conversation: { naturalness, corrections, recommendedExpressions, exchangeCount }
// - interview:    { overall, metrics, questionFeedback }
export const historyMock = [
  {
    id: 1,
    type: 'conversation',
    title: '카페에서 친구와 대화하기',
    date: '9월 8일',
    duration: 24,
    score: 92,
    summary: '전반적으로 자연스럽게 대화를 이어갔어요.',
    feedback: {
      exchangeCount: 18,
      naturalness: {
        label: '자연스러움',
        score: 92,
        description:
          '전반적으로 자연스럽게 대화를 이어갔어요. 조사 사용과 일부 표현을 조금 다듬으면 더욱 자연스러운 일본어가 됩니다.',
      },
      corrections: [
        {
          id: 1,
          original: 'コーヒー 一つ ください',
          corrected: 'コーヒーを一つください',
          note: '목적격 조사 "を"를 생략하지 않는 것이 격식 있는 표현이에요.',
        },
        {
          id: 2,
          original: 'すみません、トイレ どこ です か',
          corrected: 'すみません、トイレはどこですか?',
          note: '장소를 물을 땐 "は"로 주제를 표시하면 더 자연스러워요.',
        },
      ],
      recommendedExpressions: [
        { id: 1, jp: 'お会計、お願いします。', kr: '계산 부탁드려요.' },
        { id: 2, jp: 'また今度誘ってください。', kr: '다음에 또 불러주세요.' },
      ],
    },
  },
  {
    id: 2,
    type: 'interview',
    title: 'Backend Developer 면접 연습',
    date: '9월 7일',
    duration: 18,
    score: 86,
    summary: '경험을 직무와 잘 연결했지만 결과를 조금 더 구체적으로 설명하면 좋습니다.',
    feedback: {
      overall: {
        score: 86,
        description: '경험을 직무와 잘 연결했지만 결과를 조금 더 구체적으로 설명하면 좋습니다. 답변 흐름은 안정적이었어요.',
      },
      metrics: [
        { id: 'expression', label: '일본어 표현력', score: 84 },
        { id: 'structure', label: '답변 구조', score: 82 },
        { id: 'logic', label: '논리 / 흐름', score: 90 },
      ],
      questionFeedback: [
        {
          id: 1,
          question: 'Q1. 자기소개 및 지원 동기',
          feedback: '경력과 지원 동기를 명확하게 연결했어요. 회사 정보를 조금 더 언급하면 좋아요.',
        },
        {
          id: 2,
          question: 'Q2. 최근 진행한 프로젝트 경험',
          feedback: '기술적인 설명은 좋았지만 결과 수치가 빠져 있어 아쉬웠어요.',
        },
      ],
    },
  },
  {
    id: 3,
    type: 'conversation',
    title: '직장 동료와 점심 약속',
    date: '9월 5일',
    duration: 31,
    score: 84,
    summary: '일상적인 화제를 자연스럽게 이어갔어요.',
    feedback: {
      exchangeCount: 15,
      naturalness: {
        label: '자연스러움',
        score: 84,
        description: '일상적인 화제를 자연스럽게 이어갔어요. 몇몇 문장에서 시제 표현을 다듬으면 더 좋아집니다.',
      },
      corrections: [
        {
          id: 1,
          original: '昨日 忙しい でした',
          corrected: '昨日は忙しかったです',
          note: 'い형용사의 과거형은 「かった」로 활용해요.',
        },
      ],
      recommendedExpressions: [{ id: 1, jp: 'お先に失礼します。', kr: '먼저 가보겠습니다.' }],
    },
  },
  {
    id: 4,
    type: 'interview',
    title: '자기소개 연습',
    date: '9월 3일',
    duration: 27,
    score: 79,
    summary: '핵심 경험은 잘 짚었지만 답변을 조금 더 간결하게 구성하면 좋아요.',
    feedback: {
      overall: {
        score: 79,
        description: '핵심 경험은 잘 짚었지만 답변을 조금 더 간결하게 구성하면 좋아요. 결론을 먼저 말하는 연습을 추천해요.',
      },
      metrics: [
        { id: 'expression', label: '일본어 표현력', score: 80 },
        { id: 'structure', label: '답변 구조', score: 74 },
        { id: 'logic', label: '논리 / 흐름', score: 83 },
      ],
      questionFeedback: [
        {
          id: 1,
          question: 'Q1. 자기소개',
          feedback: '자연스러운 발음이었지만 문장이 길어져 핵심이 흐려졌어요. 결론부터 말해보세요.',
        },
      ],
    },
  },
  {
    id: 5,
    type: 'conversation',
    title: '여행지에서 길 물어보기',
    date: '9월 1일',
    duration: 19,
    score: 90,
    summary: '상황에 맞는 표현을 적절히 사용했어요.',
    feedback: {
      exchangeCount: 12,
      naturalness: {
        label: '자연스러움',
        score: 90,
        description: '상황에 맞는 표현을 적절히 사용했어요. 길 안내 관련 어휘를 폭넓게 활용했습니다.',
      },
      corrections: [
        {
          id: 1,
          original: 'この道 まっすぐ 行って ください',
          corrected: 'この道をまっすぐ行ってください',
          note: '이동 경로를 나타낼 땐 조사 "を"를 사용해요.',
        },
      ],
      recommendedExpressions: [{ id: 1, jp: 'ここから遠いですか?', kr: '여기서 먼가요?' }],
    },
  },
  {
    id: 6,
    type: 'interview',
    title: 'Frontend Developer 면접 연습',
    date: '8월 29일',
    duration: 22,
    score: 81,
    summary: '논리적인 답변이었지만 구체적인 사례를 더 들면 좋겠어요.',
    feedback: {
      overall: {
        score: 81,
        description: '논리적인 답변이었지만 구체적인 사례를 더 들면 좋겠어요. 전달력은 안정적이었습니다.',
      },
      metrics: [
        { id: 'expression', label: '일본어 표현력', score: 83 },
        { id: 'structure', label: '답변 구조', score: 78 },
        { id: 'logic', label: '논리 / 흐름', score: 82 },
      ],
      questionFeedback: [
        {
          id: 1,
          question: 'Q1. 지원 동기',
          feedback: '회사에 대한 관심은 잘 드러났지만 구체적인 사례를 덧붙이면 더 설득력 있어요.',
        },
      ],
    },
  },
]
