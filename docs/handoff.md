# 인수인계

새 세션이나 다른 컴퓨터에서 이 문서만 읽고 이어간다. 시작 지시: "`docs/handoff.md` 읽고 다음 행동 진행해."

읽는 순서: 이 문서 → [AGENTS.md](../AGENTS.md) → [plan.md](plan.md)

## 현재 상태

- 2026-11-03: 기획(PRD), 계정·설정 명세, API·ERD 계약이 B2C로 바뀌었다. 코드는 아직 B2B2C다.
- [plan.md](plan.md) MVP 계획(계정 전환 + 설정 화면)이 승인되었다. 2026-11-03 밤에 자동 개발을 시작한다.
- 병합은 사람이 한다. PR은 앞 트랙 브랜치 위에 쌓인다(plan.md "실행 순서와 PR 규칙").

## 다음 행동 (번호 순서)

1. 오케스트레이터: plan.md "실행 순서"대로 T1부터 T7까지 진행하고, 트랙마다 이 문서의 진행 표를 갱신한다.
2. 사람(아침): 아래 순서로 PR을 확인하고 병합한다. PR #16(명세) → 계획 PR → T1 → T2 → T3 → T4 → T6 → T5 → T7.
3. 사람: MVP 확인 전에 구글 OAuth 클라이언트와 카카오 테스트 앱(이메일 필수 동의, 앱 관리자 키)을 만들어 `.env`에 넣는다.

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
| 5 | T6 `backend-preferences` | 진행 중 | — | — |
| 6 | T5 `web-auth` | 대기 | — | — |
| 7 | T7 `web-settings` | 대기 | — | — |
