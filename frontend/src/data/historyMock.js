import { SessionStatus, SessionType } from './enums'

// My History mock 데이터. 실제 Backend/DB가 연결되면 이 배열을
// API 응답(예: GET /api/history)으로 교체하면 됩니다.
//
// type: SessionType.CONVERSATION | SessionType.INTERVIEW
// score: 면접(INTERVIEW)만 전체 점수를 가집니다. 일반회화(CONVERSATION)는 점수를
//        억지로 만들지 않으므로 항상 null이며, 화면에서는 미표시 처리합니다.
// sessionStatus: History에는 정상 종료(COMPLETED)된 세션만 저장됩니다. 도중에
//        나간(ABORTED) 세션은 Feedback을 만들지 않으므로 History에도 남지 않습니다.
// feedback은 각 모드의 Feedback 화면과 동일한 정보 구조를 사용합니다.
// - conversation: { stats, overallComment, naturalness, grammar, vocabulary, strengths,
//                   corrections, recommendedExpressions, nextStepTip, transcript }
// - interview:    { overall, metrics, strengths, improvements, coaching, questionFeedback }
export const historyMock = [
  {
    id: 1,
    type: SessionType.CONVERSATION,
    sessionStatus: SessionStatus.COMPLETED,
    title: '카페에서 친구와 대화하기',
    date: '9월 8일',
    duration: 24,
    score: null,
    summary: '전반적으로 자연스럽게 대화를 이어갔어요.',
    feedback: {
      exchangeCount: 18,
      overallComment: '전반적으로 자연스럽게 대화를 이어갔어요. 조사 사용과 일부 표현을 조금 다듬으면 더욱 자연스러운 일본어가 됩니다.',
      naturalness: {
        label: '자연스러움',
        description:
          '전반적으로 자연스럽게 대화를 이어갔어요. 조사 사용과 일부 표현을 조금 다듬으면 더욱 자연스러운 일본어가 됩니다.',
      },
      grammar: { label: '문법', description: '조사와 활용 대부분이 정확했어요.' },
      vocabulary: { label: '어휘', description: '상황에 맞는 표현을 폭넓게 사용했어요.' },
      strengths: ['자연스러운 속도로 대화를 이어갔어요.', '상대방 말에 적절히 리액션했어요.'],
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
      nextStepTip: '장소를 물을 때 쓰는 「は」 주제 조사를 이번 주에 의식적으로 연습해보세요.',
      transcript: [
        { id: 1, speaker: 'ai', jp: 'いらっしゃいませ。ご注文はお決まりですか?', kr: '어서오세요. 주문 정하셨나요?' },
        { id: 2, speaker: 'user', jp: 'コーヒー 一つ ください', kr: '커피 하나 주세요' },
      ],
    },
  },
  {
    id: 2,
    type: SessionType.INTERVIEW,
    sessionStatus: SessionStatus.COMPLETED,
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
        { id: 'questionFit', label: '질문 적합성', score: 87 },
        { id: 'logic', label: '논리성', score: 90 },
        { id: 'specificity', label: '구체성', score: 80 },
        { id: 'delivery', label: '전달력', score: 84 },
        { id: 'jobFit', label: '직무 적합성', score: 88 },
        { id: 'structure', label: '답변 구조', score: 82 },
        { id: 'businessJapanese', label: '비즈니스 일본어', score: 83 },
      ],
      strengths: ['경력과 지원 동기를 명확하게 연결했어요.'],
      improvements: ['결과 수치를 함께 언급하면 더 설득력 있어요.'],
      coaching: '프로젝트 경험을 말할 때 결과를 수치로 먼저 제시하는 연습을 해보세요.',
      questionFeedback: [
        {
          id: 1,
          question: 'Q1. 자기소개 및 지원 동기',
          feedback: '경력과 지원 동기를 명확하게 연결했어요. 회사 정보를 조금 더 언급하면 좋아요.',
          improvedAnswer: '貴社の技術力に魅力を感じ、これまでの経験を活かして貢献したいと考えました。',
        },
        {
          id: 2,
          question: 'Q2. 최근 진행한 프로젝트 경험',
          feedback: '기술적인 설명은 좋았지만 결과 수치가 빠져 있어 아쉬웠어요.',
          improvedAnswer: 'その結果、応答速度を約20%改善することができました。',
        },
      ],
    },
  },
  {
    id: 3,
    type: SessionType.CONVERSATION,
    sessionStatus: SessionStatus.COMPLETED,
    title: '직장 동료와 점심 약속',
    date: '9월 5일',
    duration: 31,
    score: null,
    summary: '일상적인 화제를 자연스럽게 이어갔어요.',
    feedback: {
      exchangeCount: 15,
      overallComment: '일상적인 화제를 자연스럽게 이어갔어요. 몇몇 문장에서 시제 표현을 다듬으면 더 좋아집니다.',
      naturalness: {
        label: '자연스러움',
        description: '일상적인 화제를 자연스럽게 이어갔어요. 몇몇 문장에서 시제 표현을 다듬으면 더 좋아집니다.',
      },
      grammar: { label: '문법', description: 'い형용사 과거형 활용에서 실수가 있었어요.' },
      vocabulary: { label: '어휘', description: '일상 대화에 필요한 어휘를 무난하게 사용했어요.' },
      strengths: ['자연스럽게 화제를 전환했어요.'],
      corrections: [
        {
          id: 1,
          original: '昨日 忙しい でした',
          corrected: '昨日は忙しかったです',
          note: 'い형용사의 과거형은 「かった」로 활용해요.',
        },
      ],
      recommendedExpressions: [{ id: 1, jp: 'お先に失礼します。', kr: '먼저 가보겠습니다.' }],
      nextStepTip: 'い형용사 과거형 활용을 이번 주 회화에서 반복 연습해보세요.',
      transcript: [
        { id: 1, speaker: 'user', jp: '昨日 忙しい でした', kr: '어제 바빴어요' },
        { id: 2, speaker: 'ai', jp: 'そうだったんですね、お疲れ様でした。', kr: '그러셨군요, 고생하셨어요.' },
      ],
    },
  },
  {
    id: 4,
    type: SessionType.INTERVIEW,
    sessionStatus: SessionStatus.COMPLETED,
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
        { id: 'questionFit', label: '질문 적합성', score: 82 },
        { id: 'logic', label: '논리성', score: 78 },
        { id: 'specificity', label: '구체성', score: 74 },
        { id: 'delivery', label: '전달력', score: 76 },
        { id: 'jobFit', label: '직무 적합성', score: 80 },
        { id: 'structure', label: '답변 구조', score: 72 },
        { id: 'businessJapanese', label: '비즈니스 일본어', score: 77 },
      ],
      strengths: ['핵심 경험을 빠짐없이 언급했어요.'],
      improvements: ['문장이 길어져 핵심이 흐려졌어요. 결론부터 말해보세요.'],
      coaching: '결론을 한 문장으로 먼저 말한 뒤, 이유와 사례를 덧붙이는 순서로 연습해보세요.',
      questionFeedback: [
        {
          id: 1,
          question: 'Q1. 자기소개',
          feedback: '자연스러운 발음이었지만 문장이 길어져 핵심이 흐려졌어요. 결론부터 말해보세요.',
          improvedAnswer: '結論から申し上げますと、私はバックエンド開発を専門とするエンジニアです。',
        },
      ],
    },
  },
  {
    id: 5,
    type: SessionType.CONVERSATION,
    sessionStatus: SessionStatus.COMPLETED,
    title: '여행지에서 길 물어보기',
    date: '9월 1일',
    duration: 19,
    score: null,
    summary: '상황에 맞는 표현을 적절히 사용했어요.',
    feedback: {
      exchangeCount: 12,
      overallComment: '상황에 맞는 표현을 적절히 사용했어요. 길 안내 관련 어휘를 폭넓게 활용했습니다.',
      naturalness: {
        label: '자연스러움',
        description: '상황에 맞는 표현을 적절히 사용했어요. 길 안내 관련 어휘를 폭넓게 활용했습니다.',
      },
      grammar: { label: '문법', description: '조사 사용이 대체로 정확했어요.' },
      vocabulary: { label: '어휘', description: '길 안내 관련 표현을 다양하게 사용했어요.' },
      strengths: ['필요한 정보를 정중하게 요청했어요.'],
      corrections: [
        {
          id: 1,
          original: 'この道 まっすぐ 行って ください',
          corrected: 'この道をまっすぐ行ってください',
          note: '이동 경로를 나타낼 땐 조사 "を"를 사용해요.',
        },
      ],
      recommendedExpressions: [{ id: 1, jp: 'ここから遠いですか?', kr: '여기서 먼가요?' }],
      nextStepTip: '이동/경로를 나타내는 조사 「を」 사용을 이번 주에 연습해보세요.',
      transcript: [
        { id: 1, speaker: 'user', jp: 'この道 まっすぐ 行って ください', kr: '이 길로 쭉 가주세요' },
        { id: 2, speaker: 'ai', jp: 'はい、まっすぐ行くと右側にありますよ。', kr: '네, 쭉 가시면 오른쪽에 있어요.' },
      ],
    },
  },
  {
    id: 6,
    type: SessionType.INTERVIEW,
    sessionStatus: SessionStatus.COMPLETED,
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
        { id: 'questionFit', label: '질문 적합성', score: 84 },
        { id: 'logic', label: '논리성', score: 82 },
        { id: 'specificity', label: '구체성', score: 75 },
        { id: 'delivery', label: '전달력', score: 83 },
        { id: 'jobFit', label: '직무 적합성', score: 80 },
        { id: 'structure', label: '답변 구조', score: 78 },
        { id: 'businessJapanese', label: '비즈니스 일본어', score: 79 },
      ],
      strengths: ['회사에 대한 관심이 잘 드러났어요.'],
      improvements: ['구체적인 사례를 덧붙이면 더 설득력 있어요.'],
      coaching: '주장 뒤에 항상 구체적인 사례를 하나씩 덧붙이는 습관을 만들어보세요.',
      questionFeedback: [
        {
          id: 1,
          question: 'Q1. 지원 동기',
          feedback: '회사에 대한 관심은 잘 드러났지만 구체적인 사례를 덧붙이면 더 설득력 있어요.',
          improvedAnswer: '実際に貴社のプロダクトを使ってみて、UIの使いやすさに感銘を受けました。',
        },
      ],
    },
  },
]
