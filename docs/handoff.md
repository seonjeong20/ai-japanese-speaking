# 인수인계

새 세션이나 다른 컴퓨터에서 이 문서만 읽고 이어간다. 시작 지시: "`docs/handoff.md` 읽고 다음 행동 진행해."

읽는 순서: 이 문서 → [AGENTS.md](../AGENTS.md) → [plan.md](plan.md)

## 현재 상태

- 2026-11-03: MVP 계획(plan.md T1~T7)의 자동 개발이 끝났다. 트랙마다 PR이 앞 트랙 위에 쌓여 있다(#18~#24). 병합 전이다.
- 백엔드 테스트 109개 통과, 프론트 린트·빌드 통과. 화면 동작과 실제 구글·카카오 연동은 확인하지 않았다(아래 MVP 확인 목록).
- 각 PR 본문에 완료 기준 결과, Codex 리뷰 지적과 처리, 사람이 확인할 남은 문제가 있다.

## 다음 행동 (번호 순서)

1. 사람: PR을 순서대로 확인하고 병합한다. #16(명세) → #17(계획) → #18(T1) → #19(T2) → #20(T3) → #21(T4) → #22(T6) → #23(T5) → #24(T7).
2. 사람: `~/.zshrc`에서 `JWT_SECRET`·`OPENAI_API_KEY`를 빼고 `.env`로 옮긴다(셸 값이 `.env`보다 우선함).
3. 사람: 구글 OAuth 클라이언트와 카카오 테스트 앱(이메일 필수 동의, 앱 관리자 키)을 만들고 `.env`에 키를 넣는다. redirect URI는 `http://localhost:5173/auth/callback/google`, `http://localhost:5173/auth/callback/kakao`.
4. 사람: `docker compose down -v && make up`, `cd frontend && npm run dev`로 띄워 아래 MVP 확인 목록을 직접 써 본다. 수정 목록 → decisions.md → 명세 수정 → 다시 자동 개발.
5. PR에서 결정을 기다리는 것: 이메일 대소문자 정규화 여부(#20), redirect URI 허용 목록(#20, 배포 때).

## 미처리 변경 요청 (멈출 지점에 걸린 것)

없음.

## 배포 때 주의할 것

- 운영 DB(Neon)는 새로 만든다. 탈퇴는 DB 외래 키의 `ON DELETE CASCADE`로 학습 기록을 지우는데, `ddl-auto: update`는 이미 있는 외래 키를 바꾸지 않는다. 기존 DB를 쓰면 탈퇴가 500이 된다(T4 PR). 로컬 개발 DB도 같은 이유로 `docker compose down -v && make db`로 새로 만든다.

## MVP 때 사람이 확인할 것

TC-10(로그아웃), TC-11(토큰 만료 시 첫 화면 이동), TC-13(로그인 취소), TC-17(탈퇴 취소), TC-S10(회화 설정에 기본값), TC-S11(가입 경로 표시), M-1 ~ M-5(실제 구글·카카오 연동, [specs/account.md](specs/account.md) 3장).

## 진행 표

| 순서 | 트랙 | 상태 | 리뷰 | PR |
|---|---|---|---|---|
| 1 | T1 `infra-env` | PR 생성 | Codex: 차단 0, 중요 4 (반영·확인) | #18 |
| 2 | T2 `backend-b2c-cleanup` | PR 생성 | Codex: 차단 0, 중요 2, 사소 1 (범위 예외로 기록) | #19 |
| 3 | T3 `backend-oauth-login` | PR 생성 | Codex: 차단 0, 중요 2 (1 고침, 1 근거와 함께 보류) | #20 |
| 4 | T4 `backend-account-delete` | PR 생성 | Codex: 차단 1 (운영 DB 없음, 배포 주의로 기록) | #21 |
| 5 | T6 `backend-preferences` | PR 생성 | Codex: 차단 1, 사소 1 (모두 고침) | #22 |
| 6 | T5 `web-auth` | PR 생성 | Codex: 차단 0, 사소 3 (범위 밖 수정 기록) | #23 |
| 7 | T7 `web-settings` | PR 생성 | Codex: 중요 2, 사소 1 (모두 고침) | #24 |
