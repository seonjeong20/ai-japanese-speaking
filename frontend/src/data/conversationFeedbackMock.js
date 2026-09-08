// 일반 회화 Feedback mock 데이터. 실제 AI 평가가 연결되면 이 구조 그대로
// Backend API 응답으로 교체하면 됩니다. (icon 필드는 문자열 key로 두어
// JSON 직렬화가 가능하도록 했습니다 — 아이콘 매핑은 페이지 컴포넌트에서 처리)
export const conversationFeedbackMock = {
  title: '카페에서 친구와 대화하기',
  badgeLabel: '대화 완료',
  subtitle: '오늘 대화한 내용을 AI가 분석했어요',
  stats: [
    { id: 'duration', icon: 'clock', value: '24분', label: '총 대화 시간' },
    { id: 'exchanges', icon: 'message', value: '18개', label: '주고받은 문장' },
    { id: 'corrections', icon: 'edit', value: '3개', label: '표현 교정' },
  ],
  naturalness: {
    label: '자연스러움',
    score: 88,
    description:
      '자연스럽고 유창한 표현을 잘 사용했어요! 문장 구조도 안정적이고, 일본인이 실제로 쓰는 표현을 적절히 섞어서 사용했습니다.',
  },
  corrections: [
    {
      id: 1,
      original: '私は 学生 です から',
      corrected: '学生なので',
      note: '이유를 말할 땐 "です から" 보다 "なので"가 더 자연스러워요.',
    },
    {
      id: 2,
      original: 'コーヒー 一つ ください',
      corrected: 'コーヒーを一つください',
      note: '목적격 조사 "を"를 생략하지 않는 것이 격식 있는 표현이에요.',
    },
    {
      id: 3,
      original: '私はコーヒーが飲みたいです。',
      corrected: 'コーヒーが飲みたいです。',
      note: '일상적인 대화에서는 문맥상 주어가 명확할 경우 「私は」를 생략하면 더 자연스러워요.',
    },
  ],
  recommendedExpressions: [
    { id: 1, jp: 'おすすめは何ですか?', kr: '추천 메뉴가 뭐예요?' },
    { id: 2, jp: 'それでお願いします。', kr: '그걸로 부탁드려요.' },
  ],
}
