# 설정 명세: 계정 정보와 학습 기본값 (이슈 #3)

작성일: 2026-11-03 · 상태: 초안

근거: [decisions.md](../decisions.md) 2026-11-03 "MVP 범위와 자동 개발 방식", [PRD.md](../product/PRD.md) 8.1장 "설정", GitHub 이슈 #3, [erd-api-spec.md](../contracts/erd-api-spec.md) `user_preferences`와 `/api/users/me/preferences`. 유스케이스와 테스트케이스는 기획자와 정한 내용을 Claude가 정리한 것이다.

## 1. 흐름

```
설정 화면 열기 → 계정 정보(GET /api/auth/me)와 기본값(GET /api/users/me/preferences) 표시
  → 난이도·자막 바꾸기 → [저장] (PATCH /api/users/me/preferences) → "설정이 저장되었습니다"
회화 설정 / 면접 설정 화면 열기 → 기본값(GET /api/users/me/preferences)을 미리 채움 → 이번 세션만 다른 값으로 바꿀 수 있음
```

규칙
- 계정 정보는 이름, 이메일, 가입 경로(구글·카카오)를 보여 주기만 한다. 바꿀 수 없다(가입 때 값으로 고정, account.md).
- 기본값은 난이도(`BEGINNER`, `INTERMEDIATE`, `ADVANCED`)와 자막(`OFF`, `JAPANESE`, `JAPANESE_KOREAN`) 두 가지다.
- 저장한 적이 없으면 기본값은 난이도 `INTERMEDIATE`, 자막 `JAPANESE_KOREAN`이다. 조회만으로는 DB에 행을 만들지 않는다.
- 저장은 사용자당 한 행(`user_preferences.user_id` 유일)을 만들거나 고친다.
- 기본값을 바꿔도 이미 만든 세션의 설정은 바뀌지 않는다.
- 회원 탈퇴하면 기본값도 지운다(account.md UC-8).
- 기본값을 불러오지 못하면 회화·면접 설정 화면은 위의 기본값으로 채우고 그대로 쓸 수 있다.

## 2. 유스케이스

### UC-S1 계정 정보 보기

| 칸 | 내용 |
|---|---|
| 행위자 | 로그인한 사용자 |
| 목표 | 내가 어떤 계정으로 가입했는지 본다 |
| 정상 흐름 | 사이드바 [설정] → 이름, 이메일, 가입 경로("구글로 가입" 또는 "카카오로 가입")가 보인다 |
| 사후 조건 | 바뀌는 것 없음 |

### UC-S2 학습 기본값 바꾸기

| 칸 | 내용 |
|---|---|
| 행위자 | 로그인한 사용자 |
| 목표 | 새 회화·면접을 시작할 때 쓸 난이도와 자막을 정해 둔다 |
| 정상 흐름 | 설정 화면에서 난이도·자막 선택 → [저장] → "설정이 저장되었습니다" |
| 사후 조건 | 다음에 설정 화면이나 회화·면접 설정 화면을 열면 저장한 값이 보인다. 지난 세션은 그대로다 |

### UC-S3 회화·면접을 시작할 때 기본값 쓰기

| 칸 | 내용 |
|---|---|
| 행위자 | 로그인한 사용자 |
| 목표 | 매번 고르지 않고 정해 둔 값으로 시작한다 |
| 정상 흐름 | 회화 설정(또는 면접 설정) 화면 열기 → 저장한 난이도·자막이 미리 선택되어 있다 → 필요하면 이번만 바꿔서 시작 |
| 사후 조건 | 이번에 바꾼 값은 이번 세션에만 쓰이고 기본값은 그대로다 |

## 3. 테스트케이스

| ID | 유스케이스 | 주어진 상태 | 행동 | 확인할 결과 |
|---|---|---|---|---|
| TC-S1 | UC-S1 | 구글 사용자 `홍길동`, `a@x.com` | `GET /api/auth/me` | 200, 이름·이메일·`provider: google` |
| TC-S2 | UC-S2 | 저장한 기본값 없음 | `GET /api/users/me/preferences` | 200, `INTERMEDIATE`, `JAPANESE_KOREAN`. `user_preferences` 행 0개 |
| TC-S3 | UC-S2 | 저장한 기본값 없음 | `PATCH`로 `ADVANCED`, `JAPANESE` | 200, 같은 값과 "설정이 저장되었습니다". 행 1개 |
| TC-S4 | UC-S2 | `ADVANCED`, `JAPANESE` 저장됨 | `PATCH`로 `BEGINNER`, `OFF` 후 `GET` | `BEGINNER`, `OFF`. 행은 여전히 1개 |
| TC-S5 | UC-S2 | — | `PATCH`에 `defaultDifficulty` 없음, 또는 `EXPERT` 같은 없는 값 | 400 `INVALID_REQUEST`. 저장된 값 그대로 |
| TC-S6 | UC-S2 | 사용자 A가 `ADVANCED` 저장 | 사용자 B가 `GET` | B는 `INTERMEDIATE`(A의 값이 보이지 않음) |
| TC-S7 | UC-S2 | 세션을 `BEGINNER`로 만든 뒤 | 기본값을 `ADVANCED`로 저장 | 그 세션의 난이도는 `BEGINNER` 그대로 |
| TC-S8 | 규칙 | 기본값을 저장한 사용자 | 회원 탈퇴 | `user_preferences` 행도 삭제 |
| TC-S9 | 규칙 | 로그인 안 함 | `GET`·`PATCH /api/users/me/preferences` | 401 |
| TC-S10 | UC-S3 | `ADVANCED`, `JAPANESE` 저장됨 | 회화 설정 화면 열기 | 난이도 `고급`, 자막 `일본어`가 선택되어 있다 (MVP 때 확인) |
| TC-S11 | UC-S1 | 카카오 사용자 | 설정 화면 열기 | "카카오로 가입"이 보이고 계정 정보는 바꿀 수 없다 (MVP 때 확인) |

## 4. 열린 질문

없음.
