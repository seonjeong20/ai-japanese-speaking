# 개발 계획: MVP (계정 전환 + 설정 화면)

작성일: 2026-11-03 · 상태: 승인됨 (2026-11-03, 사용자가 밤새 자동 진행을 지시)

> **오케스트레이터에게:** `handoff.md` → `AGENTS.md` → 이 문서 순서로 읽고, 아래 "실행 순서"대로 트랙을 하나씩 실행한다. 트랙 하나의 루프와 브리프 형식은 개발 지침 9.2·9.3절을 따르되, **병합하지 않고 PR까지만 만든다**(아래 "PR 규칙"). 체크박스(`- [ ]`)로 진행을 표시한다.

**목표:** 기관·매니저·관리자 기능과 이메일·비밀번호 가입을 걷어내고, 구글·카카오 소셜 로그인과 회원 탈퇴로 바꾼다. 설정 화면을 실제 API에 연결한다(이슈 #3). 이 계획이 끝나면 MVP다.

**범위 밖:** 받아쓰기·대화 AI·피드백 개선, TypeScript 전환, 배포, 튜토리얼(decisions.md 2026-11-03 "MVP 범위와 자동 개발 방식").

**구조:** 백엔드는 `auth`·`user` 패키지를 고치고 `admin`·`manager`·`organization` 패키지를 지운다. 제공자 통신은 인터페이스 뒤에 두어 테스트에서 가짜로 바꾼다. 탈퇴는 DB의 `ON DELETE CASCADE`로 사용자에 딸린 데이터를 한 번에 지운다.

**기술:** Spring Boot 3.5 / Java 21, Spring Security, `spring-boot-starter-oauth2-client`(새 의존성), PostgreSQL 16(Docker Compose), React 19 / Vite.

**명세:** [specs/account.md](specs/account.md), [specs/settings.md](specs/settings.md) · 계약: [openapi.yml](contracts/openapi.yml) `/api/auth/oauth/{provider}`, `/api/auth/me`, `DELETE /api/users/me`, `/api/users/me/preferences` · [erd.dbml](contracts/erd.dbml) `users`, `user_preferences` · 결정: [decisions.md](decisions.md) 2026-11-03 "MVP 범위와 자동 개발 방식", "계정 전환의 기술 선택", "소셜 로그인"

## 전체 규칙

- 새 의존성은 `spring-boot-starter-oauth2-client` 하나만 추가한다. 그 밖의 라이브러리는 추가하지 않는다(AGENTS.md 8장).
- `oauth2Login()`을 켜지 않는다. 프론트가 받은 인가 코드를 `POST /api/auth/oauth/{provider}`로 넘기는 방식만 쓴다.
- 제공자 토큰(구글·카카오 access token, refresh token)은 저장하지 않는다.
- 비밀값은 `.env`에만 둔다. `.env.example`에는 이름만 적는다. 키 값을 출력하지 않는다.
- 테스트에서 구글·카카오·OpenAI는 호출하지 않는다. 실제 키가 없어도 `make test`가 통과해야 한다.
- API의 제공자 값은 소문자 `google`, `kakao`다. DB `users.provider`에는 `GOOGLE`, `KAKAO`로 저장한다.
- 오류 코드와 안내 문구(계약·명세 그대로):
  - 400 `INVALID_AUTHORIZATION_CODE`
  - 403 `EMAIL_NOT_VERIFIED`, "가입할 수 없습니다. 인증된 이메일이 필요합니다"
  - 409 `EMAIL_ALREADY_REGISTERED` + `registeredProvider`, "이미 카카오로 가입된 이메일입니다. 카카오로 로그인해 주세요"(제공자 이름 자리만 바뀜)
  - 502 `OAUTH_PROVIDER_ERROR`
- 공통 검증 명령: `make test`(백엔드), `make lint`(프론트 린트 + 빌드).

## 계획에서 정한 기본값 (승인 때 확인)

명세에 없어서 계획이 정한 것이다. 바꾸려면 승인 전에 말한다.

| 항목 | 기본값 |
|---|---|
| 제공자가 이름을 주지 않을 때 | 이메일의 `@` 앞부분을 이름으로 저장 |
| 로그인 위조 방지(state) | 프론트가 무작위 `state`를 만들어 `sessionStorage`에 두고, 돌아왔을 때 다르면 첫 화면에 "로그인에 실패했습니다. 다시 시도해 주세요" |
| 로그인 후 돌아오는 주소 | `{프론트 주소}/auth/callback/{provider}` |
| 로컬 DB 포트 | 호스트 `5434` (이 컴퓨터의 5432·5433은 이미 다른 PostgreSQL이 씀) |
| 기존 로컬 DB 데이터 | 옮기지 않는다. Compose의 새 DB에서 시작한다(`users` 구조가 바뀌어 `ddl-auto: update`로는 맞출 수 없음) |

## 리뷰 중점

테스트가 직접 다루지 않지만 실제로 터지기 쉬운 곳이다. 리뷰 에이전트는 아래를 꼭 본다.

1. 같은 사람이 로그인 버튼을 두 번 눌러 같은 코드가 두 번 오면: 두 번째는 제공자가 거절하므로 400이어야 하고, 계정이 두 개 생기면 안 된다. → T3 테스트 `sameCodeTwice_secondIs400_andOneAccount`
2. 카카오 응답에 `kakao_account` 자체가 없거나 `is_email_verified`가 빠진 경우: 500이 아니라 403이어야 한다. → T3 테스트 `kakaoWithoutAccountBlock_is403`
3. 탈퇴 직후 같은 브라우저의 다른 탭이 이전 토큰으로 요청: 401이어야 한다(TC-15). → T4
4. 다른 사용자의 데이터가 탈퇴에 휩쓸리지 않는지: 면접 질문·답변처럼 서로 참조하는 표까지 확인한다. → T4 테스트 `delete_keepsOtherUsersInterviewData`
5. 실제 키 없이 앱이 뜨는지: OAuth 등록 값이 비어 있으면 Spring이 시작하지 못한다. 설정 기본값을 비어 있지 않은 자리표시자로 둔다. → T1·T3 `make test`

---

## 실행 순서와 PR 규칙

| 순서 | 트랙 | 브랜치 | PR의 기준(base) 브랜치 |
|---|---|---|---|
| 1 | T1 `infra-env` | `track/t1-infra-env` | `docs/account-plan` |
| 2 | T2 `backend-b2c-cleanup` | `track/t2-backend-b2c-cleanup` | `track/t1-infra-env` |
| 3 | T3 `backend-oauth-login` | `track/t3-backend-oauth-login` | `track/t2-backend-b2c-cleanup` |
| 4 | T4 `backend-account-delete` | `track/t4-backend-account-delete` | `track/t3-backend-oauth-login` |
| 5 | T6 `backend-preferences` | `track/t6-backend-preferences` | `track/t4-backend-account-delete` |
| 6 | T5 `web-auth` | `track/t5-web-auth` | `track/t6-backend-preferences` |
| 7 | T7 `web-settings` | `track/t7-web-settings` | `track/t5-web-auth` |

- 사람이 아침에 병합하므로 main에 병합하지 않는다. 각 트랙은 바로 앞 트랙 브랜치에서 갈라져 나오고, PR의 기준 브랜치도 앞 트랙 브랜치다. 사람은 PR #16 → 계획 PR → T1 → … 순서로 병합한다.
- 트랙 하나: 브랜치 생성 → 구현(구현 에이전트) → 완료 기준 실행 → 리뷰(구현과 다른 모델) → 차단 지적 수정 → `make test`(프론트 트랙은 `make lint`) → 커밋·푸시 → `gh pr create` → `handoff.md` 진행 표 갱신.
- PR 본문: 트랙 이름, 완료 기준 각 줄의 결과, 리뷰 지적과 처리, 남은 문제. 실패한 완료 기준이 있으면 숨기지 않고 적는다.
- 멈출 지점: 계약 변경이 필요하거나, 수정 두 번 뒤에도 완료 기준을 못 채우면 그 트랙 PR을 초안(draft)으로 올리고 이유를 `handoff.md` "미처리 변경 요청"에 적은 뒤 다음 트랙으로 간다. 다음 트랙이 그 트랙에 의존해 진행할 수 없으면 거기서 멈춘다.
- 소유권: 한 번에 한 트랙만 돌므로 겹침은 문제가 되지 않는다. 각 트랙은 자기 "파일" 목록 밖을 고치지 않는다. 꼭 필요하면 PR 본문 "남은 문제"에 적는다.

### T1 `infra-env`: Docker Compose와 `.env` 읽기

**파일**
- 만들기: `docker-compose.yml`, `backend/Dockerfile`, `backend/.dockerignore`, `.env.example`, `Makefile`
- 고치기: `backend/src/main/resources/application.yml`

**다른 트랙에 주는 것**
- `.env` 이름: `DB_URL`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `OPENAI_API_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `KAKAO_REST_API_KEY`, `KAKAO_CLIENT_SECRET`, `KAKAO_ADMIN_KEY`, `VITE_GOOGLE_CLIENT_ID`, `VITE_KAKAO_REST_API_KEY`
- 설정 속성: `kakao.admin-key: ${KAKAO_ADMIN_KEY:unset}` (T4가 읽음)
- 명령: `make db`(DB만), `make up`(DB + 백엔드), `make test`, `make lint`

**정한 것**
- `application.yml` 맨 위에 `spring.config.import: optional:file:../.env[.properties]`. 작업 폴더가 `backend/`일 때 저장소 루트의 `.env`를 읽는다.
- Compose `db`: `postgres:16`, 호스트 `5434` → 컨테이너 `5432`, 이름 있는 볼륨, `pg_isready` 상태 확인. `POSTGRES_DB/USER/PASSWORD`는 `.env`의 `DB_NAME/DB_USERNAME/DB_PASSWORD`로 채운다.
- Compose `backend`: `./backend`를 빌드, `env_file: .env`, `DB_URL`만 `jdbc:postgresql://db:5432/${DB_NAME}`로 덮어쓴다, `db`가 건강해진 뒤 시작, `8080` 공개.
- `Dockerfile`: 2단계. `eclipse-temurin:21-jdk`에서 `./gradlew bootJar -x test`, `eclipse-temurin:21-jre`에서 실행.
- `Makefile`: `make test` = `docker compose up -d --wait db` 후 `cd backend && ./gradlew test`. `make lint` = `cd frontend && npm run lint && npm run build`.

**완료 기준**
- [ ] `.env.example`에 위 이름이 모두 있고 값은 비어 있다. 필수 6개(DB·JWT)는 `이름=`, 선택 8개(OpenAI·구글·카카오)는 `# 이름=`로 주석 처리 (빈 값 줄이 있으면 기본값이 무시되어 앱이 시작하지 못하므로)
- [ ] `make db` 후 `docker compose ps db`가 `healthy`다
- [ ] `.env`에 DB·JWT 값만 채운 상태에서 `make test`를 돌리면 지금 있는 테스트가 모두 통과한다(이 트랙은 테스트 내용을 고치지 않는다)
- [ ] `make up` 후 `curl -s -o /dev/null -w '%{http_code}' localhost:8080/api/auth/me`가 `401`이다 (백엔드에 상태 확인이 없어 뜨는 데 몇 초 걸리므로 20초까지 2초 간격으로 다시 시도)
- [ ] `git status`에 `.env`가 나타나지 않는다

---

### T2 `backend-b2c-cleanup`: B2B2C 코드 걷어내기와 사용자 구조 바꾸기

**파일**
- 지우기: `admin/`, `manager/`, `organization/` 패키지 전체와 그 테스트. `auth/dto/LoginRequest`, `SignupRequest`, `SignupResponse`, `SignupRole`, `OrganizationOptionResponse`. `auth/exception/EmailAlreadyExistsException`, `InvalidCredentialsException`, `OrganizationNotAvailableException`, `AccountNotActiveException`. `user/exception/InvalidUserStatusTransitionException`. `common/dto/UserStatusChangeResponse`. `AuthService.signup/login`, `AuthController`의 `/signup`, `/login`. `PasswordEncoder` 빈.
- 고치기: `user/entity/User.java`, `UserRole.java`, `UserStatus.java`, `UserRepository.java`, `auth/dto/UserResponse.java`, `auth/dto/LoginResponse.java`, `auth/security/SecurityConfig.java`, `common/exception/GlobalExceptionHandler.java`(지운 예외의 처리기 제거)
- 만들기: `user/entity/OAuthProvider.java`, `backend/src/test/java/com/aijapanese/speaking/support/TestUsers.java`
- 테스트 고치기: 대화·면접·기록 테스트의 사용자 만드는 부분을 `TestUsers`로 바꾼다. 대상은 `ConversationServiceIntegrationTest`, `HistoryServiceIntegrationTest`, `InterviewServiceIntegrationTest`, `InterviewAnswerServiceIntegrationTest`, `InterviewCompleteServiceIntegrationTest`, `InterviewAbortServiceIntegrationTest`. 이 중 `MANAGER`·`ADMIN` 접근을 확인하는 테스트(`InterviewServiceIntegrationTest`의 7번·8번, 143~166행)는 역할이 없어졌으므로 지운다. `Organization`, `PasswordEncoder`, `PENDING`·`INACTIVE`·`REJECTED` 상태를 쓰는 부분도 같은 방식으로 지우거나 바꾼다. `AuthAccessControlIntegrationTest`는 지우고 아래 완료 기준 테스트로 바꾼다.

**다른 트랙에 주는 것**
- `enum OAuthProvider { GOOGLE, KAKAO }`: JSON은 소문자(`@JsonValue`), 경로 변수 `google`·`kakao`를 받는 변환기 포함
- `enum UserRole { LEARNER }`, `enum UserStatus { ACTIVE }`
- `User(OAuthProvider provider, String providerUserId, String name, String email, LocalDateTime createdAt)`: 역할 `LEARNER`, 상태 `ACTIVE`로 만든다. `getProvider()`, `getProviderUserId()`, `updateLastLoginAt(LocalDateTime)`
- `UserRepository.findByProviderAndProviderUserId(OAuthProvider, String): Optional<User>`, `findByEmail(String): Optional<User>`
- `record UserResponse(Long id, String name, String email, OAuthProvider provider, UserRole role, UserStatus status)` + `from(User)`
- `record LoginResponse(String accessToken, UserResponse user, boolean newUser)`
- `TestUsers.learner(UserRepository repo, String suffix): User`: 구글 사용자 하나를 저장해 돌려준다
- `SecurityConfig`: `/api/auth/oauth/**` 허용, 나머지 `/api/**`는 로그인 필요. 역할별 경로 규칙(`/api/manager/**`, `/api/admin/**`)은 없앤다. `/api/conversations/**`, `/api/interviews/**`, `/api/history/**`의 `LEARNER` 규칙은 남긴다

**정한 것**
- `users` 열은 ERD 그대로: `provider`, `provider_user_id`, `name`, `email`(유일), `role`, `status`, `created_at`, `updated_at`, `last_login_at`. (`provider`, `provider_user_id`)에 유일 제약.
- 비밀번호·기관·부서 열은 엔티티에서 없앤다.

**완료 기준**
- [ ] `grep -rniE 'organization|manager|admin|password' backend/src`가 아무것도 찾지 않는다
- [ ] `UserRepositoryTest.providerAndId_isUnique`: 같은 (`GOOGLE`, `g-1`)을 두 번 저장하면 `DataIntegrityViolationException`
- [ ] `UserRepositoryTest.sameIdDifferentProvider_isAllowed`: (`GOOGLE`, `g-1`)과 (`KAKAO`, `g-1`)은 둘 다 저장된다 (TC-12의 저장소 부분)
- [ ] `AuthMeIntegrationTest.me_returnsProvider`: `TestUsers.learner`의 토큰으로 `GET /api/auth/me` → 200, `provider`가 `"google"`, `role`이 `"LEARNER"`, `organization`·`department` 필드 없음
- [ ] `AuthMeIntegrationTest.me_withoutToken_is401`
- [ ] `AuthMeIntegrationTest.me_withInvalidToken_is401` (TC-11 백엔드 부분): 서명이 틀린 토큰과 만료 시각이 지난 토큰 모두 401. 첫 화면 이동은 MVP 때 확인
- [ ] 대화·면접·기록 테스트가 고친 뒤에도 모두 통과한다
- [ ] `make test` 통과

---

### T5 `web-auth`: 소셜 로그인 화면, 탈퇴 화면, B2B2C 화면 걷어내기

**파일**
- 지우기: `pages/Admin/`, `pages/ManagerDashboard/`, `pages/ManagerLearners/`, `pages/ManagerSettings/`, `components/admin/`, `components/manager/`, `components/layout/AdminLayout.jsx`, `ManagerLayout.jsx`, `components/auth/SignUpForm.jsx`, `SignInForm.jsx`, `api/admin.js`, `api/manager.js`, `api/organizations.js`, `data/adminMock.js`, `data/managerMock.js`
- 고치기: `App.jsx`(매니저·관리자 경로 제거, `/auth/callback/:provider` 추가), `data/enums.js`(`Role`은 `LEARNER`만, `OAuthProvider` 추가), `api/auth.js`, `pages/Auth/AuthPage.jsx`, `pages/Settings/SettingsPage.jsx`, `components/layout/Sidebar.jsx`, `vite.config.js`
- 만들기: `pages/AuthCallback/AuthCallbackPage.jsx`, `api/users.js`, `lib/oauth.js`

**쓰는 것 (계약)**
- `POST /api/auth/oauth/{provider}` `{ code, redirectUri }` → `{ accessToken, user, newUser }`
- `DELETE /api/users/me` → 204

**정한 것**
- `vite.config.js`에 `envDir: '..'`. 저장소 루트 `.env`의 `VITE_` 값만 읽는다.
- `lib/oauth.js`: `buildAuthorizeUrl(provider): string`, `consumeState(received): boolean`. 구글은 `scope=openid email profile`, 카카오는 `scope=account_email profile_nickname`. `redirect_uri`는 `${window.location.origin}/auth/callback/${provider}`.
- `api/auth.js`: `oauthLogin(provider, code, redirectUri)`가 성공하면 `setSession` 후 `{ user, newUser }`를 돌려준다.
- `api/users.js`: `deleteMe()`가 성공하면 `clearSession()`.
- 콜백 화면: 주소에 `error`가 있으면 첫 화면으로 보내고 "로그인이 취소되었습니다". `state`가 다르면 "로그인에 실패했습니다. 다시 시도해 주세요". 409면 `registeredProvider`로 "이미 카카오로 가입된 이메일입니다. 카카오로 로그인해 주세요", 403이면 "가입할 수 없습니다. 인증된 이메일이 필요합니다". 성공하면 `/dashboard`. 첫 화면 안내는 이동할 때 `state`로 넘긴다.
- 설정 화면: 비밀번호 변경을 없애고 [회원 탈퇴]를 둔다. 확인은 화면 안의 확인 창으로 한다(`window.confirm` 쓰지 않음). 지워지는 내용(계정, 일반 회화 기록, 면접 질문·답변·평가, 피드백, 설정)을 보여 주고, 구글 사용자에게는 "구글 계정 설정의 '서드 파티 앱 및 서비스'에서 연결을 직접 끊을 수 있어요"를 함께 보여 준다. [취소]는 아무것도 하지 않는다.

**완료 기준**
- [ ] `grep -rniE 'manager|admin|organization|password|signup' frontend/src`가 아무것도 찾지 않는다
- [ ] `make lint` 통과 (린트 + 빌드)
- 화면 동작(TC-10, TC-13, TC-17)은 MVP 때 사람이 확인한다 (decisions.md 2026-11-03)

---

### T3 `backend-oauth-login`: 소셜 로그인 API

**파일**
- 고치기: `backend/build.gradle`(의존성 추가), `application.yml`(`spring.security.oauth2.client` 블록), `auth/controller/AuthController.java`, `auth/service/AuthService.java`, `common/exception/GlobalExceptionHandler.java`
- 만들기:
  - `auth/oauth/SocialProfile.java`: `record SocialProfile(OAuthProvider provider, String providerUserId, String email, boolean emailVerified, String name)`
  - `auth/oauth/SocialProfileClient.java`: `interface`, `SocialProfile fetch(OAuthProvider provider, String code, String redirectUri)`
  - `auth/oauth/SpringSocialProfileClient.java`: 위 인터페이스 구현
  - `auth/dto/OAuthLoginRequest.java`: `record(String code, String redirectUri)`. 빈 값 검사는 `@NotBlank`가 아니라 `AuthService`에서 하고 `InvalidAuthorizationCodeException`을 던진다(계약상 "인가 코드가 없음"도 `INVALID_AUTHORIZATION_CODE`)
  - `auth/exception/InvalidAuthorizationCodeException`, `OAuthProviderException`, `EmailNotVerifiedException`, `EmailAlreadyRegisteredException(OAuthProvider registeredProvider)`
  - `common/dto/EmailAlreadyRegisteredErrorResponse.java`: `record(String code, String message, OAuthProvider registeredProvider)`
- 테스트: `auth/SocialLoginIntegrationTest.java` (`SocialProfileClient`를 `@MockitoBean`으로 바꿈)

**쓰는 것:** T2의 `User`, `UserRepository`, `UserResponse`, `LoginResponse`, `OAuthProvider`, `JwtTokenProvider.generateToken(Long, UserRole)`

**다른 트랙에 주는 것:** `AuthService.oauthLogin(OAuthProvider provider, OAuthLoginRequest request): LoginResponse`

**정한 것**
- 설정: `registration.google`은 Spring 기본 제공자 + `scope: openid,email,profile`. `registration.kakao`는 `client-authentication-method: client_secret_post`, `authorization-grant-type: authorization_code`, `scope: account_email,profile_nickname`. `provider.kakao`는 `authorization-uri: https://kauth.kakao.com/oauth/authorize`, `token-uri: https://kauth.kakao.com/oauth/token`, `user-info-uri: https://kapi.kakao.com/v2/user/me`, `user-name-attribute: id`. 모든 키는 `${이름:unset}`처럼 비어 있지 않은 기본값을 둔다.
- 구현: `ClientRegistrationRepository`에서 등록을 찾고, 요청의 `code`·`redirectUri`로 `OAuth2AuthorizationExchange`를 만들어 `RestClientAuthorizationCodeTokenResponseClient`로 교환한 뒤 `DefaultOAuth2UserService`로 사용자 정보를 받는다.
- 응답 읽기: 구글은 `sub`, `email`, `email_verified`, `name`. 카카오는 `id`, `kakao_account.email`, `kakao_account.is_email_verified`, `kakao_account.profile.nickname`. 빠진 값은 `null`·`false`로 둔다.
- 오류 바꾸기: 교환 응답의 오류 코드가 `invalid_grant`·`invalid_request`면 `InvalidAuthorizationCodeException`(400), 그 밖의 제공자 오류와 연결 실패는 `OAuthProviderException`(502).
- `oauthLogin` 순서(명세 1장 그림 그대로):
  1. (제공자, 고유 ID)로 계정을 찾으면 `lastLoginAt`만 갱신하고 `newUser=false`. 이름·이메일은 바꾸지 않는다.
  2. 없으면 이메일이 없거나 `emailVerified=false`면 `EmailNotVerifiedException`.
  3. 같은 이메일 계정이 있으면 `EmailAlreadyRegisteredException(그 계정의 제공자)`.
  4. 새 `User`를 저장하고 `newUser=true`.

**완료 기준** (테스트 이름: 명세 TC)
- [ ] `googleFirstLogin_createsLearner` (TC-1): 200, 계정 1개, 제공자 `GOOGLE`, 고유 ID `g-1`, 이름 `홍길동`, `newUser=true`, `accessToken` 있음
- [ ] `kakaoFirstLogin_createsLearner` (TC-2): 200, 제공자 `KAKAO`, 고유 ID `k-1`
- [ ] `returningUser_logsIn_updatesLastLogin` (TC-3): 계정 수 그대로, `newUser=false`, `lastLoginAt` 갱신
- [ ] `returningUser_keepsSignupNameAndEmail` (TC-4): 저장된 이메일 `a@x.com`, 이름 `홍길동` 그대로
- [ ] `googleWithKakaoEmail_is409` (TC-5): 409, `code=EMAIL_ALREADY_REGISTERED`, `registeredProvider="kakao"`, 계정 수 그대로
- [ ] `kakaoWithGoogleEmail_is409` (TC-6): 409, `registeredProvider="google"`
- [ ] `googleUnverifiedEmail_is403` (TC-7): 403, `code=EMAIL_NOT_VERIFIED`, 계정 수 그대로
- [ ] `kakaoNoEmail_is403` (TC-8)
- [ ] `kakaoUnverifiedEmail_is403` (TC-9)
- [ ] `kakaoWithoutAccountBlock_is403` (리뷰 중점 2)
- [ ] `sameIdDifferentProvider_createsNewAccount` (TC-12): (`GOOGLE`, `g-1`)이 있을 때 (`KAKAO`, `g-1`, `c@x.com`) → 새 계정
- [ ] `invalidCode_is400`: 가짜 클라이언트가 `InvalidAuthorizationCodeException`을 던지면 400 `INVALID_AUTHORIZATION_CODE`
- [ ] `sameCodeTwice_secondIs400_andOneAccount` (리뷰 중점 1)
- [ ] `providerDown_is502`: `OAuthProviderException`이면 502 `OAUTH_PROVIDER_ERROR`
- [ ] `blankCode_is400`: `code`가 빈 문자열이거나 없으면 400 `INVALID_AUTHORIZATION_CODE`, 가짜 클라이언트 호출 없음
- [ ] `unknownProvider_is400`: `POST /api/auth/oauth/naver` → 400
- [ ] `SpringSocialProfileClientTest.kakaoAttributes_mapToProfile`: 카카오 사용자 정보 JSON 예시를 넣으면 `SocialProfile`의 다섯 값이 맞게 나온다(네트워크 없이 변환 부분만)
- [ ] `make test` 통과

---

### T4 `backend-account-delete`: 회원 탈퇴 API

**파일**
- 만들기:
  - `user/controller/UserController.java`: `DELETE /api/users/me` → 204
  - `user/service/UserAccountService.java`: `void delete(Long userId)`
  - `user/kakao/KakaoUnlinkClient.java`: `interface`, `void unlink(String kakaoUserId)`
  - `user/kakao/RestKakaoUnlinkClient.java`: `POST https://kapi.kakao.com/v1/user/unlink`, 헤더 `Authorization: KakaoAK {kakao.admin-key}`, 폼 `target_id_type=user_id`, `target_id={회원번호}`
- 고치기: `speaking/entity/SpeakingSession`, `SpeakingMessage`, `conversation/entity/ConversationSetting`, `ConversationFeedback`, `ConversationCorrection`, `interview/entity/InterviewSetting`, `InterviewFeedback`, `InterviewQuestion`, `InterviewAnswer`, `InterviewAnswerFeedback`, `InterviewEvaluationScore`의 부모 연결(`@ManyToOne`·`@OneToOne`)마다 `@OnDelete(action = OnDeleteAction.CASCADE)`
- 테스트: `user/AccountDeleteIntegrationTest.java` (`KakaoUnlinkClient`를 `@MockitoBean`으로 바꿈)

**쓰는 것:** T2의 `User`, `UserRepository`, `TestUsers`

**정한 것**
- 순서: 카카오 사용자면 `unlink`를 부르고, 실패(예외)해도 기록만 남기고 계속한다 → 사용자 행을 지운다. DB가 사용자에 딸린 세션·메시지·피드백·교정·설정·면접 데이터를 함께 지운다.
- 구글 사용자는 연결 끊기를 부르지 않는다.
- `user_preferences`는 T6이 엔티티를 만들면서 `@OnDelete(CASCADE)`와 탈퇴 테스트(TC-S8)를 함께 넣는다. 이 트랙은 다루지 않는다.
- 테스트는 삭제 뒤 `flush`·`clear`하고 저장소 조회로 남은 행을 센다.
- 새 예외를 만들지 않는다(`GlobalExceptionHandler`는 T3 소유).

**완료 기준**
- [ ] `delete_removesUserAndAllLearningData` (TC-14): 일반 회화 세션·메시지·피드백·교정·설정, 면접 질문·답변·평가가 있는 구글 사용자 탈퇴 → 204, 그 사용자 것은 모두 0건, `unlink` 호출 없음
- [ ] `delete_keepsOtherUsersInterviewData` (리뷰 중점 4): 다른 사용자의 세션·면접 데이터 수 그대로
- [ ] `oldTokenAfterDelete_is401` (TC-15): 탈퇴 뒤 이전 토큰으로 `GET /api/auth/me` → 401
- [ ] `rejoinAfterDelete_createsNewAccount` (TC-16): 탈퇴 뒤 같은 (`GOOGLE`, `g-1`)로 `User`를 새로 저장할 수 있고, 새 `id`의 세션 수는 0
- [ ] `kakaoDelete_callsUnlinkWithMemberId` (TC-19): `unlink("k-1")` 한 번 호출, 계정 삭제
- [ ] `kakaoUnlinkFails_stillDeletes` (TC-18): `unlink`가 예외를 던져도 204, 계정과 기록 삭제
- [ ] `deleteWithoutToken_is401`
- [ ] `make test` 통과

---

### T6 `backend-preferences`: 학습 기본값 API (이슈 #3)

**파일**
- 만들기 (새 패키지 `preference/`):
  - `preference/entity/UserPreference.java`: 표 `user_preferences`. `@OneToOne` `user`(열 `user_id`, 유일, `@OnDelete(CASCADE)`), `defaultDifficulty`(`conversation.entity.Difficulty`), `defaultSubtitleMode`(`conversation.entity.SubtitleMode`), `createdAt`, `updatedAt`
  - `preference/repository/UserPreferenceRepository.java`: `Optional<UserPreference> findByUser_Id(Long userId)`
  - `preference/dto/UserPreferencesResponse.java`: `record(Difficulty defaultDifficulty, SubtitleMode defaultSubtitleMode, String message)`, `message`가 `null`이면 JSON에서 뺀다(`@JsonInclude(NON_NULL)`)
  - `preference/dto/UpdateUserPreferencesRequest.java`: `record(@NotNull Difficulty defaultDifficulty, @NotNull SubtitleMode defaultSubtitleMode)`
  - `preference/service/UserPreferenceService.java`: `UserPreferencesResponse get(Long userId)`, `UserPreferencesResponse update(Long userId, UpdateUserPreferencesRequest request)`
  - `preference/controller/UserPreferenceController.java`: `GET`·`PATCH /api/users/me/preferences`
- 테스트: `preference/UserPreferenceIntegrationTest.java`

**쓰는 것:** T2의 `User`, `UserRepository`, `TestUsers`. T4의 `DELETE /api/users/me`

**다른 트랙에 주는 것:** 계약 그대로 `GET`·`PATCH /api/users/me/preferences`

**정한 것**
- 저장한 적이 없으면 `get`은 `INTERMEDIATE`, `JAPANESE_KOREAN`을 돌려주고 행을 만들지 않는다.
- `update`는 행이 없으면 만들고 있으면 고친다. 응답 `message`는 "설정이 저장되었습니다."
- 없는 값·빠진 값은 기존 처리기대로 400 `INVALID_REQUEST`.

**완료 기준** (테스트 이름: 명세 TC)
- [ ] `get_withoutSaved_returnsDefaults_andNoRow` (TC-S2)
- [ ] `patch_createsRow` (TC-S3): 200, `ADVANCED`, `JAPANESE`, `message`, 행 1개
- [ ] `patch_twice_updatesSameRow` (TC-S4)
- [ ] `patch_missingOrUnknownValue_is400` (TC-S5): 저장된 값 그대로
- [ ] `get_isPerUser` (TC-S6)
- [ ] `patch_doesNotChangeExistingSessionSetting` (TC-S7)
- [ ] `deleteAccount_removesPreferences` (TC-S8): 기본값 저장 → `DELETE /api/users/me` → 행 0개
- [ ] `withoutToken_is401` (TC-S9): `GET`과 `PATCH` 모두
- [ ] `make test` 통과

---

### T7 `web-settings`: 설정 화면과 회화·면접 설정 화면 연결 (이슈 #3)

**파일**
- 고치기: `frontend/src/api/users.js`(T5가 만든 파일에 함수 추가), `pages/Settings/SettingsPage.jsx`, `pages/ConversationSetup/ConversationSetupPage.jsx`, `pages/InterviewSetup/InterviewSetupPage.jsx`, `data/settingsMock.js`
- 고치기(추가): `frontend/src/api/auth.js`에 `getMe()`

**쓰는 것 (계약):** `GET /api/auth/me`, `GET`·`PATCH /api/users/me/preferences`

**정한 것**
- `api/auth.js`: `getMe(): Promise<User>`. `api/users.js`: `getPreferences()`, `updatePreferences({ defaultDifficulty, defaultSubtitleMode })`.
- 설정 화면: 계정 정보는 `getMe()`로 이름·이메일·가입 경로("구글로 가입" / "카카오로 가입")를 읽기 전용으로 보여 준다. 기본값은 `getPreferences()`로 채우고 [저장]에서 `updatePreferences()`, 성공하면 "설정이 저장되었습니다". T5가 넣은 [회원 탈퇴]는 그대로 둔다.
- 회화·면접 설정 화면: 열 때 `getPreferences()`로 난이도·자막을 미리 채운다. 실패하면 `INTERMEDIATE`, `JAPANESE_KOREAN`으로 채우고 화면은 그대로 쓸 수 있다.
- `settingsMock.js`에서 `accountMock`을 지운다. `defaultSpeakingSettings`는 실패할 때 쓰는 기본값으로만 남긴다.

**완료 기준**
- [ ] `grep -rn 'accountMock' frontend/src`가 아무것도 찾지 않는다
- [ ] `grep -rn 'getPreferences' frontend/src/pages`가 설정·회화 설정·면접 설정 세 화면에서 찾는다
- [ ] `make lint` 통과
- 화면 동작(TC-S10, TC-S11)은 MVP 때 사람이 확인한다

**이 트랙의 마지막 커밋 (오케스트레이터)**
- [ ] `make test`, `make lint` 모두 통과
- [ ] `docs/README.md`, `architecture.md`의 "코드는 아직 B2B2C" 안내를 지운다
- [ ] `handoff.md`에 MVP 확인 목록을 남긴다: TC-10, TC-11(첫 화면 이동), TC-13, TC-17, TC-S10, TC-S11, M-1 ~ M-5. 구글·카카오 앱 등록과 `.env` 키 채우기는 사람이 한다
