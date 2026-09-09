# AGENTS.md — Coding Agent 작업 매뉴얼

이 문서는 Claude/Codex 등 Coding Agent가 이 프로젝트에서 작업할 때 **무엇을 먼저 읽고, 어떤 범위까지만 작업하고, 어떻게 검증하고, 어떻게 보고해야 하는가**를 정의하는 작업 매뉴얼이다.

이 문서는 Architecture Decision이나 Backend Implementation Rule을 정의하지 않는다. 그 책임은 각각 [`docs/architecture.md`](docs/architecture.md), [`docs/backend-rules.md`](docs/backend-rules.md)에 있다. 이 문서는 Coding Agent가 그 문서들을 반드시 읽고 따르도록 연결하는 절차서다.

---

## 1. Source of Truth / 문서 우선순위

작업 시작 전 아래 순서로 관련 문서를 확인한다.

1. `AGENTS.md` (이 문서)
2. [`docs/architecture.md`](docs/architecture.md) — 전체 아키텍처와 설계 결정
3. [`docs/backend-rules.md`](docs/backend-rules.md) — Spring Boot 구현 규칙
4. 작업과 관련된 ERD/API specification ([`docs/ai-japanese-speaking-erd-api-spec.md`](docs/ai-japanese-speaking-erd-api-spec.md)) — 데이터 모델과 기능/API 설계 기준
5. [`docs/ai-japanese-speaking-openapi.yml`](docs/ai-japanese-speaking-openapi.yml) — API endpoint, Request/Response, Enum 등의 API Contract
6. 현재 구현 코드 — 위 계약을 실제로 구현한 결과물

문서와 코드가 충돌하거나 문서끼리 충돌하면 Coding Agent가 임의로 어느 한쪽을 선택해 수정하지 않는다. 불일치를 보고하고 사람의 결정을 기다린다.

## 2. Bounded Task Rule

요청받은 작업 범위만 구현한다.

예: "Organization/User Entity를 구현해줘"라는 요청에 대해 Auth 전체 구현, JWT 구현, Manager API 구현, AI 기능 구현, Docker 설정, unrelated refactoring 등으로 임의로 확장하지 않는다.

하나의 작업 단위를 완료하고 검증한 뒤 다음 작업으로 넘어간다. 사용자가 명시적으로 여러 작업을 함께 요청한 경우에만 그 범위까지 작업한다.

## 3. Read Before Implement

구현 전에 해당 작업과 관련된 계약을 먼저 확인한다. 프롬프트만 보고 구현하지 않는다.

예:
- User Entity 구현 시: ERD의 `users` 정의, `backend-rules.md`의 Entity/JPA 규칙, OpenAPI의 관련 Enum(Role/Status 등) 확인
- Auth API 구현 시: OpenAPI Auth endpoint, `architecture.md`의 Security 결정, `backend-rules.md`의 Authentication/Security 규칙 확인

## 4. Do Not Guess Undecided Policies

문서에서 미확정으로 표시된 사항을 구현 편의를 위해 임의로 결정하지 않는다. 특히 `backend-rules.md`의 "현재 미확정 사항" 절을 반드시 확인한다 (JWT expiration, Refresh Token, logout/revoke 정책, LLM Provider/Model, Interview Agent 세부 구조, Prompt 설계, Structured Output 세부 Schema, STT/TTS Provider, AI retry/timeout 정책, 도메인 간 세부 의존성 규칙 등).

현재 작업에 미확정 결정이 반드시 필요하다면 추측해서 구현하지 말고 보고한다.

보고 예시:

> "현재 작업을 진행하려면 JWT expiration 결정이 필요합니다. `backend-rules.md`에서는 해당 정책이 미확정 상태입니다. 구현 전에 결정이 필요합니다."

## 5. Contract Change Prohibition

구현 편의를 위해 기존 계약을 임의로 변경하지 않는다. 특히 다음을 임의 변경하지 않는다.

- OpenAPI endpoint, HTTP Method, Request/Response Schema, Enum, HTTP Status 계약
- ERD Table, ERD Column, FK, Constraint
- User Role/Status 정책, Authentication/Authorization 정책
- `architecture.md`의 확정된 Architecture Decision
- `backend-rules.md`의 확정된 Implementation Rule

변경이 필요하다고 판단되면 직접 수정하지 않고 먼저 보고한다.

## 6. Contract Conflict Procedure

구현 중 문서/코드 간 불일치를 발견할 수 있다 (예: OpenAPI는 `/api/interviews`인데 코드는 `/api/interview`).

이 경우 임의로 하나를 선택해 수정하지 않고 다음 순서를 따른다.

1. 불일치를 발견한다.
2. 어떤 문서/코드가 충돌하는지 명시한다.
3. 구현에 미치는 영향을 설명한다.
4. 가능한 해결 방향이 있다면 제안할 수 있다.
5. 사람이 결정할 때까지 계약을 변경하지 않는다.

## 7. No Unrequested Refactoring

현재 작업과 관계없는 코드를 수정하지 않는다.

금지 예: 요청하지 않은 package 이동, unrelated class/method rename, 기존 구조 전면 refactoring, dependency 정리, formatter를 이용한 프로젝트 전체 변경, "더 좋은 구조"라는 이유만으로 기존 구현 교체.

필요성을 발견하면 변경하지 말고 보고한다.

## 8. Dependency / Technology Rule

새로운 dependency, library, infrastructure를 임의로 추가하지 않는다. 특히 `backend-rules.md`에서 아직 채택하지 않은 기술(Lombok, MapStruct, Flyway, Redis, Docker, QueryDSL, Testcontainers, Message Queue 등)을 임의로 도입하지 않는다.

새 기술이 실제로 필요하다고 판단되면 다음을 먼저 보고하고 사람의 결정을 기다린다.

1. 필요한 이유
2. 해결하려는 문제
3. 기존 기술만으로 가능한지
4. 도입 시 영향 범위

별도 Python AI Server는 `architecture.md`의 현재 결정에 따라 사용하지 않는다.

## 9. Secret Handling

실제 Secret(JWT Secret, Database Password, LLM API Key, STT/TTS API Key 등)을 코드나 문서에 작성하지 않는다. 예제 값이 필요한 경우 실제 Secret처럼 보이는 값을 만들지 말고 명확한 placeholder를 사용한다.

## 10. Implementation Verification

각 bounded task 구현 후 가능한 범위에서 검증한다.

최소 확인: compile/build, 해당 작업과 관련된 test, 변경된 파일 확인, 기존 계약 위반 여부 확인.

검증하지 않은 내용을 "정상 동작한다"고 보고하지 않는다. 테스트 또는 build가 실패하면 실패 사실을 숨기지 않고 다음을 함께 보고한다: 무엇이 실패했는지, 오류의 핵심 원인, 현재 작업 범위에서 수정 가능한지, 추가 결정이 필요한지.

## 11. Testing Scope

테스트는 현재 작업 범위와 관련된 부분을 우선한다. 작은 작업 하나를 위해 프로젝트 전체 테스트 구조를 임의로 새로 설계하지 않는다.

테스트 도구나 라이브러리를 새로 도입해야 하는 경우 먼저 보고한다. 구체적인 테스트 전략이 아직 문서에 정의되지 않았다면 대규모 테스트 정책을 임의로 확정하지 않는다.

## 12. Documentation Changes

구현하면서 `architecture.md`, `backend-rules.md`, ERD, OpenAPI를 편의상 수정하지 않는다. 문서 변경이 필요한 경우 먼저 보고하고 사람이 변경을 승인한 뒤 진행한다.

단, 사용자가 특정 문서 수정을 명시적으로 요청한 경우에는 요청된 범위 내에서 수정할 수 있다.

## 13. Completion Report

작업 완료 후 장황한 설명 대신 다음 형식으로 보고한다.

```markdown
## 작업 내용
- 무엇을 구현/수정했는지

## 변경 파일
- 생성/수정한 파일

## 검증 결과
- 실행한 build/test
- 성공/실패 여부

## 남은 문제 / 결정사항
- 발견한 충돌
- 미확정 사항
- 다음 작업 전에 결정해야 할 내용
```

문제가 없다면 "없음"이라고 명시한다.

## 14. AI-native Development Principle

사람과 Coding Agent의 역할을 다음처럼 구분한다.

**사람**: 요구사항 결정, Architecture 결정, API/DB Contract 결정, 정책 결정, 변경 승인

**Coding Agent**: 문서와 계약을 읽음, 요청받은 bounded task 구현, 테스트/빌드, 계약과 구현의 불일치 발견, 결과 보고

Coding Agent는 설계 결정을 대신하는 것이 아니라, 확정된 설계와 계약을 기반으로 구현을 수행한다.
