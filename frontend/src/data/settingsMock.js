import { Difficulty, SubtitleMode } from './enums'

// 학습 기본값을 불러오지 못했을 때 쓰는 값입니다. 서버가 "저장한 적 없음"일 때 돌려주는
// 값(INTERMEDIATE, JAPANESE_KOREAN)과 같습니다. 실제 값은 GET /api/users/me/preferences로 읽습니다.
export const defaultSpeakingSettings = {
  difficulty: Difficulty.INTERMEDIATE,
  subtitleMode: SubtitleMode.JAPANESE_KOREAN,
}
