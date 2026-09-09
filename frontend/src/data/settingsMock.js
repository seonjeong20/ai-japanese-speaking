import { Difficulty, SubtitleMode } from './enums'

// 계정 정보는 현재 조회 전용 mock 데이터입니다. 실제 Backend가 연결되면
// 로그인한 사용자 정보로 교체하면 됩니다.
export const accountMock = {
  name: 'skala',
  email: 'skala@example.com',
  organization: 'SKALA Academy',
}

// Speaking 학습 기본 설정의 초기값입니다. GET/PATCH /api/users/me/preferences 응답과
// 동일한 enum 값을 사용합니다. Conversation Setup / Interview Setup 화면은 이 값을
// 새로운 Speaking을 시작할 때의 기본값으로 불러오고, 세션 한정으로 다른 값을 선택할 수 있습니다.
export const defaultSpeakingSettings = {
  difficulty: Difficulty.INTERMEDIATE,
  subtitleMode: SubtitleMode.JAPANESE_KOREAN,
}
