# AI 일본어 Speaking 서비스: Backend Implementation Rules

기준일: 2026-09-09

[`architecture.md`](architecture.md)가 "백엔드를 어떤 구조로 설계하는가"를 정의한다면, 이 문서는 "실제 Spring Boot 코드를 작성할 때 어떤 구현 규칙을 지켜야 하는가"를 정의한다. Claude/Codex 등 Coding Agent가 백엔드를 구현할 때 반드시 참고하는 구현 규칙이다.

계약 기준 문서:

- 아키텍처 결정: [`architecture.md`](architecture.md)
- ERD/API 기준: [`ai-japanese-speaking-erd-api-spec.md`](ai-japanese-speaking-erd-api-spec.md)
- OpenAPI 명세: [`ai-japanese-speaking-openapi.yml`](ai-japanese-speaking-openapi.yml)

이 문서는 위 문서의 결정을 구현 규칙으로 구체화할 뿐, 새로운 아키텍처/보안/DB/API 정책을 확정하지 않는다. 이 문서와 위 문서가 충돌하면 위 문서가 우선하며, 그런 충돌을 발견하면 Coding Agent는 임의로 고치지 않고 보고한다.

---

## 1. Layer Rules

기본 구조: `Controller → Service → Repository`

### Controller

책임: HTTP Request 수신, Request DTO validation, Service 호출, Response DTO 반환.

규칙:
- Controller에 Business Logic을 작성하지 않는다.
- Controller에서 Repository를 직접 호출하지 않는다.
- Entity를 Request/Response로 직접 사용하지 않는다.
- 인증된 사용자를 판단하기 위해 클라이언트가 전달한 `userId`를 신뢰하지 않는다.

### Service

책임: Business Logic, Transaction 관리, 도메인 규칙 검증, Business Authorization, Repository 호출, Entity ↔ DTO 변환에 필요한 orchestration.

기관 소속 확인처럼 데이터와 도메인 규칙이 필요한 권한 검증은 Service에서 처리한다.

### Repository

책임: Database 접근.

규칙:
- Repository에 Business Logic을 작성하지 않는다.
- Spring Data JPA를 사용한다.

---

## 2. Entity / JPA Rules

기존 ERD가 Database Contract의 기준이다.

규칙:
- ERD에 정의된 Table / Column / FK를 기준으로 Entity를 작성한다.
- 구현 편의를 위해 Entity/Table/Column/FK를 임의로 추가하거나 삭제하지 않는다.
- 필요한 FK 관계는 JPA 연관관계로 표현한다.
- `@ManyToOne`, `@OneToOne` 연관관계는 기본적으로 LAZY loading을 사용한다.
- 연관관계는 단방향을 우선한다.
- 꼭 필요한 경우가 아니라면 편의를 위해 양방향 관계를 만들지 않는다.
- `@OneToMany`를 편의 목적으로 무조건 추가하지 않는다.
- 실제 ERD 관계와 맞지 않는 JPA 관계를 만들지 않는다.

구체적인 도메인 간 의존성 규칙([`architecture.md`](architecture.md) 2장 참고)은 아직 확정하지 않았으므로 이 문서에서 임의로 추가하지 않는다.

---

## 3. Primary Key Rules

MVP에서는 Entity PK에 다음 원칙을 사용한다.

- Java 타입: `Long`
- DB가 ID를 생성한다.
- 기본적으로 `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 사용한다.

UUID 등의 다른 ID 전략은 현재 도입하지 않는다.

단, 기존 ERD가 다른 형태를 명시하고 있다면 ERD를 우선한다.

---

## 4. Enum Rules

Java Enum은 DB에 문자열로 저장한다.

기본 규칙: `@Enumerated(EnumType.STRING)` 사용. `EnumType.ORDINAL`은 사용하지 않는다.

예: `LEARNER`/`MANAGER`/`ADMIN`, `PENDING`/`ACTIVE`/`REJECTED`/`INACTIVE`, `BEGINNER`/`INTERMEDIATE`/`ADVANCED` 등 기존 ERD/OpenAPI에 정의된 enum 값을 그대로 사용한다.

규칙:
- enum 이름을 임의로 변경하지 않는다.
- enum 값을 임의로 추가/삭제하지 않는다.
- ERD/OpenAPI와 Java Enum 값이 일치해야 한다.

---

## 5. DTO Rules

Entity와 API DTO를 분리한다. Request DTO와 Response DTO를 목적에 따라 분리한다.

예: `SignupRequest`, `LoginRequest`, `LoginResponse`, `UserResponse`

규칙:
- Entity를 Controller의 Request/Response로 직접 사용하지 않는다.
- OpenAPI Schema를 DTO 계약의 기준으로 사용한다.
- OpenAPI에 없는 필드를 편의상 Response DTO에 추가하지 않는다.
- API 의미가 드러나는 DTO 이름을 사용한다.

Java 21을 사용하는 경우 단순 데이터 전달용 DTO는 `record` 사용을 우선 고려한다. 단, 모든 DTO를 무조건 record로 만들라는 의미는 아니며 기술적으로 적합하지 않은 경우 일반 class를 사용할 수 있다.

---

## 6. Validation Rules

Validation을 두 종류로 구분한다.

### Request 형식 검증 — DTO validation

예: 필수값, 빈 문자열, 이메일 형식, 문자열 길이 등.

필요한 경우 Jakarta Validation(`@NotBlank`, `@NotNull`, `@Email`, `@Size` 등)을 사용한다. Controller에서는 `@Valid`를 사용한다.

### Business Validation — Service

예: 이미 가입된 이메일인가? 선택한 Organization이 ACTIVE인가? 해당 Learner가 Manager와 같은 Organization인가? 현재 상태에서 해당 작업이 가능한가?

원칙: 형식 검증 → DTO Validation / 비즈니스 규칙 검증 → Service

---

## 7. Exception Handling Rules

Controller마다 반복적인 try/catch를 작성하지 않는다. 전역 예외 처리를 사용한다.

기본 구조: `@RestControllerAdvice`를 사용하는 `GlobalExceptionHandler`.

API Error Response의 정확한 구조는 기존 OpenAPI의 `ErrorResponse` Schema를 기준으로 한다.

규칙:
- OpenAPI에 정의된 `ErrorResponse` 구조를 임의로 변경하지 않는다.
- Domain/Business Exception을 적절한 HTTP Status와 `ErrorResponse`로 변환한다.
- Controller마다 같은 예외 처리 코드를 반복하지 않는다.

구체적인 Error Code 목록이 기존 명세에 없다면 Coding Agent가 임의로 대규모 Error Code 체계를 설계하지 않는다. 필요한 경우 먼저 보고한다.

---

## 8. Authentication / Security Rules

Spring Security + JWT를 사용한다.

### Password

- 비밀번호 평문 저장 금지.
- `PasswordEncoder`를 사용해 hash한 값만 DB에 저장한다.
- 로그 등에 password를 출력하지 않는다.

### Authenticated User

인증된 사용자 정보는 JWT / Spring Security Context를 기준으로 한다.

규칙:
- 프론트엔드가 전달한 `userId`를 현재 사용자 식별 용도로 신뢰하지 않는다.
- 현재 사용자 ID/Role은 Security Context에서 가져온다.

### Authorization

- Role 기반 접근 제어(`LEARNER`/`MANAGER`/`ADMIN`) → Spring Security
- Business Authorization(예: Manager가 자신의 Organization에 속한 Learner만 관리할 수 있는지 확인) → Service

### Secrets

다음을 소스 코드에 하드코딩하지 않는다: JWT Secret, LLM API Key, STT/TTS API Key, Database Password, 기타 Secret. 환경 설정/환경 변수를 통해 주입한다.

아직 확정하지 않은 다음 정책은 이 문서에서 임의로 결정하지 않는다. 구현 직전에 별도로 결정한다.
- JWT 만료 시간
- Refresh Token 도입 여부
- Token rotation
- 세부 logout/token revoke 정책

---

## 9. Transaction Rules

Transaction 경계는 Service 계층을 기준으로 둔다.

규칙:
- 여러 DB 변경이 하나의 Business Operation을 구성하면 Service에서 하나의 Transaction으로 처리한다.
- Controller에서 Transaction을 관리하지 않는다.
- Repository에서 Business Transaction을 정의하지 않는다.

`@Transactional`의 세부 사용 위치와 readOnly 정책을 과도하게 규칙화하지 말고, 실제 Use Case 구현 시 필요한 범위에서 적용한다.

---

## 10. API Contract Rules

기존 OpenAPI YAML([`ai-japanese-speaking-openapi.yml`](ai-japanese-speaking-openapi.yml))이 API Contract의 기준이다.

Coding Agent는 임의로 다음을 변경하지 않는다: Endpoint, HTTP Method, Path parameter, Query parameter, Request Schema, Response Schema, Enum, HTTP Status 계약.

구현 중 OpenAPI와 실제 요구사항이 충돌하면 다음 순서를 따른다.

```text
구현 중단 → 충돌 내용 보고 → 사람이 결정 → 명세 수정 → 구현 재개
```

---

## 11. Database Contract Rules

기존 ERD([`ai-japanese-speaking-erd-api-spec.md`](ai-japanese-speaking-erd-api-spec.md), [`erd_v2.pdf`](erd_v2.pdf))가 Database Contract의 기준이다.

Coding Agent는 임의로 다음을 변경하지 않는다: Table 추가/삭제, Column 추가/삭제, Column 의미 변경, FK 변경, Unique Constraint 변경, Enum 의미 변경, Role/Status 데이터 정책 변경.

필요성을 발견하면 직접 변경하지 않고 먼저 보고한다.

최종적으로 `ERD ↔ JPA Entity ↔ 실제 PostgreSQL Schema`가 일치해야 한다.

---

## 12. AI Implementation Boundary

[`architecture.md`](architecture.md) 12장에 정의된 AI 구조를 따른다.

```text
Business Service → Domain AI Service → Common AI Client → Spring AI → External LLM
```

규칙:
- Business Service에서 외부 LLM Provider를 직접 호출하지 않는다.
- Conversation AI와 Interview AI의 도메인 로직을 하나의 Service에 섞지 않는다.
- 구체적인 Prompt / Agent 구조 / Structured Output / 모델 선택은 아직 확정하지 않는다. 해당 내용은 추후 `ai-design.md`에서 정의한다.

Coding Agent가 AI 구현 단계 전에 임의로 모델이나 Agent 구조를 확정하지 않는다.

---

## 13. Speech Implementation Boundary

STT/TTS는 LLM 책임과 분리한다([`architecture.md`](architecture.md) 14장 참고).

구체적인 STT/TTS Provider는 아직 결정하지 않았다. 따라서 현재 Coding Agent는:
- 특정 Provider를 아키텍처 결정으로 확정하지 않는다.
- Provider-specific 코드 구조를 미리 설계하지 않는다.

Speech 구현 단계에서 별도로 결정한다.

---

## 14. Coding Agent Change Rules

Coding Agent는 구현 편의를 이유로 계약을 임의 변경하지 않는다.

특히 다음 변경은 금지한다.
- OpenAPI endpoint 임의 변경
- Request/Response Schema 임의 변경
- Enum 임의 변경
- ERD Table/Column 임의 변경
- FK/Constraint 임의 변경
- User Role/Status 정책 임의 변경
- Authentication/Authorization 정책 임의 변경
- 아직 확정하지 않은 AI/STT/TTS 정책 임의 결정

변경이 필요하다고 판단되면 다음을 따른다.

1. 구현을 진행하기 전에 문제를 보고한다.
2. 변경이 필요한 이유를 설명한다.
3. 영향받는 ERD/OpenAPI/architecture 문서를 명시한다.
4. 사람이 결정할 때까지 계약을 변경하지 않는다.

---

## 15. 현재 미확정 사항

다음 항목은 의도적으로 미확정 상태로 남긴다. Coding Agent는 구현 편의를 위해 아래 항목을 임의로 결정하지 않는다. 결정이 필요해지면 먼저 보고하고 사람의 판단을 받는다.

- JWT expiration
- Refresh Token 도입 여부
- logout/revoke 세부 정책
- 구체적인 LLM Provider/Model
- Interview Agent 세부 구조 (Question Generator / Analyzer / Evaluator / Reflection / Coach / Follow-up)
- Prompt 설계
- Structured Output 세부 Schema
- STT Provider
- TTS Provider
- AI retry/timeout 세부 정책
- 도메인 간 세부 의존성 규칙

---

## 16. 아직 채택하지 않은 기술

다음 기술/라이브러리는 이 문서에서 새로 채택하지 않는다. 나쁘다는 뜻이 아니라 아직 채택 여부를 결정하지 않았기 때문이다. 도입이 필요하다고 판단되면 먼저 보고한다.

- Lombok
- MapStruct
- Flyway
- Redis
- Docker
- QueryDSL
- Testcontainers
- 별도 Python AI Server
- Message Queue
