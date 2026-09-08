// 계정 정보는 현재 조회 전용 mock 데이터입니다. 실제 Backend가 연결되면
// 로그인한 사용자 정보로 교체하면 됩니다.
export const accountMock = {
  name: 'skala',
  email: 'skala@example.com',
  organization: 'SKALA Academy',
}

// Speaking 학습 기본 설정의 초기값입니다. 향후 Conversation Setup / Interview Setup
// 화면이 이 값을 기본값으로 사용할 예정입니다.
export const defaultSpeakingSettings = {
  difficulty: '중급',
  subtitleMode: '일본어 + 한국어',
}
