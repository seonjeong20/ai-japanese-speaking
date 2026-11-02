# 설계 결정 기록

이 프로젝트에서 내린 결정과 그 이유를 기록한다. 최신 결정을 위에 쓰고, 바뀐 결정은 지우지 않고 제목에 "— 위 결정으로 대체됨"을 붙인다.

## 2026-11-02 서비스 형태 B2B2C → B2C 전환 (사용자 결정)

### 기존 구현 (B2B2C, 태그 `v1.0-b2b2c`)

기관(Organization)이 서비스를 도입하고, 기관에 소속된 학습자가 쓰는 구조로 개발했다.

- 역할 3개: `users.role`이 `LEARNER | MANAGER | ADMIN`이고, 별도 Entity 없이 Role로 구분한다(`docs/architecture/architecture.md` 9장).
  - Learner: 일반 회화·면접 회화 학습, 본인 학습 기록 조회
  - Manager: 같은 기관 소속 학습자의 승인·거절·활성화·비활성화, 기관 이용 현황 Dashboard
  - Admin: 기관 관리, Manager 승인·상태 관리, 서비스 전체 이용 현황 Dashboard
- 권한: Role 검증은 Spring Security가, "같은 기관 소속인지" 같은 업무 규칙은 Service가 검증한다.
- 백엔드에 `organization/`, `manager/`, `admin/` 패키지가 있고, 프론트엔드에 Manager·Admin 화면과 API 모듈(`manager.js`, `admin.js`, `organizations.js`)이 있다.
- 학습자는 기관 승인을 거쳐야 학습할 수 있다.

### 결정

| 결정 | 이유 | 대안 |
|---|---|---|
| 서비스를 개인 사용자가 바로 가입해 쓰는 단순한 B2C 형태로 바꾼다 | 기관·매니저 승인 구조 없이 실제 서비스로 작동하게 하고 싶다 | B2B2C 구조 유지 |
| 기존 B2B2C 구현은 지우지 않고 남긴다. 마지막 상태를 태그 `v1.0-b2b2c`로 보관한다 | 이전 기획과 구현도 내 작업의 흔적이라 남겨 두고 싶다 | 기존 코드를 삭제하고 B2C로 새로 작성 |

- 영향: PRD, ERD, OpenAPI, `architecture.md`, `backend-rules.md`, 인증·권한, Manager·Admin 화면.
- 열린 질문 (B2C 기획에서 정한다)
  - Manager·Organization 기능을 코드에서 어떻게 둘지 (비활성화, 별도 패키지로 격리, 삭제 등)
  - 서비스 운영자용 Admin 기능을 남길지, 남긴다면 범위
  - 가입 직후 바로 학습할 수 있게 할지 (승인 절차 제거 여부)
  - 남아 있는 이슈 #3(Settings 페이지 실 API 연동)을 B2C 기준으로 다시 정의할지

## 2026-11-02 API 키 등 비밀값 관리를 셸 환경변수에서 `.env` 파일로 전환 (사용자 결정)

### 기존 구현

- 백엔드 `application.yml`이 `${OPENAI_API_KEY}`, `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${JWT_SECRET}`을 OS 환경변수로 읽는다.
- README는 터미널에서 `export`로 값을 넣도록 안내하고, 실제로는 `~/.zshrc`에 API 키를 넣어 실행했다.
- 루트의 `.env`는 연습 계정 정보 참고용이고 애플리케이션이 읽지 않는다.

### 결정

| 결정 | 이유 | 대안 |
|---|---|---|
| 비밀값을 `~/.zshrc`가 아니라 프로젝트의 `.env` 파일에 두고, 키 이름만 적은 `.env.example`을 커밋한다 (Day-0_zaeho 방식) | `~/.zshrc`에 키를 두면 작업 환경(컴퓨터)이 바뀔 때마다 다시 설정해야 해서 번거롭다 | `~/.zshrc`의 `export` 유지 |

- 영향: 백엔드 설정 로딩, README 실행 방법, `.gitignore`.
- 열린 질문: Spring Boot에서 `.env`를 읽는 방법(실행 스크립트·IDE 설정·라이브러리 등)은 개발 계획에서 정한다. 새 라이브러리가 필요하면 AGENTS.md 8장에 따라 따로 결정한다.

## 2026-11-02 프론트엔드 TypeScript·Zustand 부분 도입 (사용자 결정)

### 기존 구현

- React 19 + Vite, JavaScript(`.jsx`/`.js`)로 작성했고 TypeScript 설정이 없다.
- 로그인 토큰과 사용자 정보는 `frontend/src/api/client.js`에서 `localStorage`로 저장·조회한다. 전역 상태 관리 라이브러리는 없다.
- 회화 진행 상태는 각 화면 컴포넌트 안에서 관리한다.

### 결정

| 결정 | 이유 | 대안 |
|---|---|---|
| 프론트엔드에 TypeScript를 **점진적으로** 도입한다. `tsconfig.json`에 `allowJs: true`, `checkJs: false`, `strict: true`를 두어 기존 `.jsx`는 그대로 두고, 바꾼 파일만 타입 검사한다. 타입 검사는 `npm run typecheck`(`tsc --noEmit`)로 따로 돌린다 | TypeScript를 공부하기 위해서다. 한 번에 전부 바꾸지 않고 파일 단위로 익힌다. Vite 개발 서버는 타입 오류가 있어도 실행되므로 검사 명령을 분리한다 | 프론트엔드 전체를 한 번에 TS로 전환 |
| 먼저 바꿀 대상은 세 가지다: API 응답 타입(AI 회화 응답, 면접 평가 결과의 점수·피드백·코칭 → `types/conversation.ts`), `useAudioRecorder`(반환값과 녹음 상태 `'idle' \| 'requesting' \| 'recording' \| 'error'`), 면접 회화 화면(질문 → 답변 → 평가 → 코칭 단계를 유니온 타입으로) | 응답 필드 누락처럼 타입이 없어 놓친 문제를 찾는다. 녹음 상태를 타입으로 정하면 예전 마이크 권한 버그 같은 잘못된 상태 전이를 구조로 막을 수 있다 | — |
| Zustand는 **로그인 사용자 정보와 역할**에만 쓴다. 회화 진행 상태는 지금처럼 각 화면 컴포넌트 안에 둔다 | 사용자·역할은 역할별 라우팅과 대시보드 등 여러 화면이 같이 쓴다. 두 회화 화면의 상태를 독립적으로 관리하기로 한 기존 설계와 맞춘다 | 회화 진행 상태까지 전역 상태로 관리 |

- 적용 순서
  1. Day 1: TS 설정(`tsconfig.json`, `src/vite-env.d.ts`, `typecheck` 스크립트), `main.jsx` → `main.tsx`, 위 세 대상 전환
  2. Day 2: Zustand로 로그인 사용자·역할 상태 이전
  3. Day 3: 커밋을 나눠 남기고, README에 "TS/Zustand 전환" 섹션을 추가한다
- 전환하면서 기록할 것: 타입을 붙이며 발견한 문제, 전역/지역 상태를 나눈 기준, 막혔던 점과 해결 방법. 자기소개서 소재로도 쓴다.
- 영향: `frontend/package.json`에 `typescript`, `@types/react`, `@types/react-dom`(dev), `zustand`를 추가했다.
- 확인 필요: 역할을 "학습자/기관/관리자"로 두는 것은 B2B2C 기준이다. 위 B2C 전환의 열린 질문이 정해지면 Zustand에 둘 역할 범위를 다시 정한다.
