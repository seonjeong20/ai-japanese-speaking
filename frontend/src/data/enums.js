// 서비스 전역에서 사용하는 enum 정의입니다. 실제 값(코드)은 Backend/DB와 동일한
// 영문 enum을 그대로 사용하고, 화면에 보여줄 때만 Label 매핑을 통해 한국어로 변환합니다.
// (DB/API 값 자체를 한국어 문자열로 사용하지 않습니다.)

export const Role = {
  LEARNER: 'LEARNER',
}

export const UserStatus = {
  ACTIVE: 'ACTIVE',
}

export const UserStatusLabel = {
  ACTIVE: '활성',
}

// 소셜 로그인 제공자입니다. API 경로와 응답 값은 소문자(google, kakao)입니다.
export const OAuthProvider = {
  GOOGLE: 'google',
  KAKAO: 'kakao',
}

export const OAuthProviderLabel = {
  google: '구글',
  kakao: '카카오',
}

export const SessionType = {
  CONVERSATION: 'CONVERSATION',
  INTERVIEW: 'INTERVIEW',
}

export const SessionTypeLabel = {
  CONVERSATION: '일반 회화',
  INTERVIEW: '면접 회화',
}

export const SessionStatus = {
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  ABORTED: 'ABORTED',
}

export const Difficulty = {
  BEGINNER: 'BEGINNER',
  INTERMEDIATE: 'INTERMEDIATE',
  ADVANCED: 'ADVANCED',
}

export const DifficultyLabel = {
  BEGINNER: '초급',
  INTERMEDIATE: '중급',
  ADVANCED: '고급',
}

// SegmentedControl(난이도 선택)에서 바로 사용할 수 있는 {value, label} 목록입니다.
export const DIFFICULTY_OPTIONS = Object.values(Difficulty).map((value) => ({
  value,
  label: DifficultyLabel[value],
}))

export const SubtitleMode = {
  OFF: 'OFF',
  JAPANESE: 'JAPANESE',
  JAPANESE_KOREAN: 'JAPANESE_KOREAN',
}

export const SubtitleModeLabel = {
  OFF: '자막 없음',
  JAPANESE: '일본어',
  JAPANESE_KOREAN: '일본어 + 한국어',
}

// SegmentedControl(자막 선택)에서 바로 사용할 수 있는 {value, label} 목록입니다.
export const SUBTITLE_MODE_OPTIONS = Object.values(SubtitleMode).map((value) => ({
  value,
  label: SubtitleModeLabel[value],
}))

export const QuestionKind = {
  INITIAL: 'INITIAL',
  FOLLOW_UP: 'FOLLOW_UP',
}
