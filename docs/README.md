# 문서 색인

문서는 현재 상태만 적는다. 바뀐 이력은 git과 [decisions.md](decisions.md)에 남긴다. B2B2C 버전의 문서는 태그 `v1.0-b2b2c`에서 볼 수 있다.

## 정본

| 무엇 | 문서 | 비고 |
|---|---|---|
| 기획 (무엇을 왜) | [product/PRD.md](product/PRD.md) | 근거: [raw-notes.md](product/raw-notes.md)(기획자 원문), [problem.md](product/problem.md)(문제 정의), [research.md](product/research.md)(시장·경쟁·선행 연구·소셜 로그인 조사) |
| 결정과 이유 | [decisions.md](decisions.md) | 최신 결정이 위. 바뀐 결정은 "대체됨" 표시 |
| 백엔드 구조 결정 | [architecture/architecture.md](architecture/architecture.md) | 그림: [architecture/images/](architecture/images/) (PNG, draw.io 원본) |
| AI·STT·TTS 설계 | [architecture/ai-design.md](architecture/ai-design.md) | |
| 백엔드 구현 규칙, 미확정 사항 | [rules/backend-rules.md](rules/backend-rules.md) | |
| ERD와 API 설계 기준 | [contracts/erd-api-spec.md](contracts/erd-api-spec.md) | |
| API 계약 | [contracts/openapi.yml](contracts/openapi.yml) | |
| ERD 원본 (DBML) | [contracts/erd.dbml](contracts/erd.dbml) | |
| 기능 명세 (흐름·유스케이스·테스트케이스) | [specs/](specs/) | [account.md](specs/account.md): 소셜 로그인·회원 탈퇴, [settings.md](specs/settings.md): 설정 화면 |
| 개발 계획 (트랙·소유권·완료 기준) | [plan.md](plan.md) | 사람이 승인한 뒤 오케스트레이터가 따른다 |
| 인수인계 (현재 상태·다음 행동) | [handoff.md](handoff.md) | 자동 개발의 시작점 |

계약 문서(ERD·OpenAPI)와 설계 문서가 충돌하면 계약 문서가 우선한다. 작업 절차와 문서 우선순위는 [AGENTS.md](../AGENTS.md)를 따른다.

B2C 전환([decisions.md](decisions.md) 2026-11-02)에 따라 위 문서들을 B2C 목표 구조로 개정했다(2026-11-03). 코드는 아직 B2B2C 상태이며 개발 단계에서 계약에 맞춘다.

## 정본이 아닌 문서

| 폴더 | 문서 | 용도 |
|---|---|---|
| [notes/](notes/) | [design-study-notes.md](notes/design-study-notes.md) | "왜 이렇게 나눴나" 설계 복습 노트 (학습용, 기준 아님) |
| [archive/](archive/) | `kotoba-개요.pdf` | 발표용 개요 자료 보관 |
