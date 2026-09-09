# AI 일본어 Speaking 서비스: ERD + API 기준 문서

기준일: 2026-09-09  
근거: [웹 프로젝트 기획](chatgpt-conversation://6a99632e-1628-83ee-a66d-e376c3dc74ff) 대화의 최종 합의와 최신 업로드 `Untitled (1).pdf`(2페이지, 14개 테이블). PDF의 컬럼·관계를 확인하고 대화의 DBML로 UNIQUE·기본값·상태 의미를 대조했다.

이 문서는 1차 설계 기준이다. 예시의 ID·문장·날짜·점수는 샘플이며, 모든 DTO의 필수 여부·HTTP 상태 코드·오류 계약이 완성되었다는 뜻은 아니다. 대화에서 미정인 부분은 마지막 절에 모았다. 이전 제안보다 뒤에 확정한 내용을 우선한다.

## 목차

1. 확정 정책과 권한
2. ERD 관계와 전체 컬럼
3. 상태 및 데이터 규칙
4. 전체 API 목록
5. Auth / User
6. Speaking / Conversation / Interview
7. History / Manager / Admin
8. AI 출력과 DB 매핑
9. 미정 사항 및 프론트 수정

## 1. 확정 정책과 권한

| 역할 | 기관 소속 | 가입·승인 | 허용 범위 |
|---|---|---|---|
| LEARNER | 기존 기관 필수 | 가입 PENDING → 같은 기관 Manager 승인 | 본인의 Speaking, Feedback, History, 기본 설정 |
| MANAGER | 기존 기관 필수 | 가입 PENDING → Admin 승인 | 같은 기관 학습자 승인·상태 관리, 기관 이용 집계 |
| ADMIN | organization_id = NULL | 공개 가입 불가. 해시 비밀번호로 DB 초기 생성 | 기관 관리, Manager 승인·상태 관리, 서비스 전체 집계 |

- 회원가입은 `LEARNER`, `MANAGER`만 허용한다. `ADMIN` 요청은 서버에서 차단하며 MVP 관리자 생성 API도 없다.
- 기관은 Admin이 먼저 생성한다. 가입 시 기관명 자유 입력이나 기관 자동 생성을 하지 않는다.
- 가입 선택 목록에는 `ACTIVE` 기관만 반환한다.
- Manager는 같은 기관 학습자의 개별 답변·Speaking 상세·AI Feedback을 조회할 수 없다. 해당 상세 API를 만들지 않는다.
- Admin의 합의 범위는 기관·Manager 관리와 전체 이용현황이다. 학습자 원문 열람 권한을 추가로 부여하지 않는다.
- 개인 세션·피드백·History 접근 시 로그인 사용자와 세션 소유자가 같아야 한다.
- 학습 요청의 사용자 ID는 검증된 JWT 인증 정보에서 얻는다. 클라이언트가 보낸 userId를 세션 소유자로 사용하지 않는다.
- 계정은 ACTIVE 상태에서 로그인한다. PENDING, REJECTED, INACTIVE는 이용을 제한한다.

```text
Admin 기관 생성 → ACTIVE 기관이 가입 선택 목록에 표시
  ├─ Manager 가입(PENDING) → Admin 승인 → ACTIVE
  └─ Learner 가입(PENDING) → 해당 기관 Manager 승인 → ACTIVE
```

## 2. ERD 관계와 전체 컬럼

표의 `pk`는 기본키, `increment`는 자동 증가, `not null`은 필수, `unique`는 중복 금지다. 별도 필수 표기가 없는 컬럼은 NULL을 허용한다. DB 컬럼의 NULL 허용 여부와 API 입력 필수 여부는 구분한다.

### 관계 요약

| 부모 | 자식 FK | 관계·의미 |
|---|---|---|
| organizations | users.organization_id | 기관 1 : 사용자 N. Admin은 소속 없음 |
| users | user_preferences.user_id UNIQUE | 사용자당 기본 설정 최대 1행 |
| users | speaking_sessions.user_id | 사용자 1 : 학습 N |
| speaking_sessions | conversation_settings.session_id UNIQUE | 일반회화 세션 설정 최대 1행 |
| speaking_sessions | interview_settings.session_id UNIQUE | 면접 세션 설정 최대 1행 |
| speaking_sessions | speaking_messages.session_id | 세션 1 : 발화 N |
| speaking_sessions | conversation_feedbacks.session_id UNIQUE | 일반회화 전체 피드백 최대 1행 |
| conversation_feedbacks | conversation_corrections.feedback_id | 피드백 1 : 교정 N |
| speaking_messages | conversation_corrections.message_id | 선택적 원문 연결. 메시지 1 : 교정 N |
| speaking_sessions | interview_questions.session_id | 세션 1 : 질문 N |
| interview_questions | interview_questions.parent_question_id | 부모 질문 1 : 꼬리질문 N |
| interview_answers | interview_questions.source_answer_id | 근거 답변 1 : 생성 질문 N |
| interview_questions | interview_answers.question_id UNIQUE | 질문당 확정 답변 최대 1행 |
| interview_answers | interview_answer_feedbacks.answer_id UNIQUE | 답변당 상세 피드백 최대 1행 |
| interview_answer_feedbacks | interview_evaluation_scores.answer_feedback_id | 피드백 1 : 항목별 점수 N |
| speaking_sessions | interview_feedbacks.session_id UNIQUE | 면접 세션 전체 평가 최대 1행 |

`최대 1행`은 생성 전에는 0행일 수 있다는 뜻이다. 세션 유형에 맞는 settings 하나만 생성한다. 개별 FK와 UNIQUE만으로 두 settings의 상호 배타성이 자동 보장되지는 않으므로 모드별 처리에서 지킨다.

추가 복합 UNIQUE:
- `speaking_messages(session_id, sequence_no)`
- `interview_evaluation_scores(answer_feedback_id, criterion)`

### organizations

기관명과 운영 상태. Admin이 먼저 생성하며 회원가입 시 기존 기관을 선택한다.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `name` | `varchar(100)` | not null |
| `status` | `varchar(20)` | not null, default: 'ACTIVE' |
| `created_at` | `timestamp` | not null |
| `updated_at` | `timestamp` | NULL 허용 |

### users

Learner·Manager·Admin의 공통 계정과 승인 상태. Admin은 기관 없이 초기 데이터로 생성한다.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `organization_id` | `bigint` | NULL 허용 |
| `name` | `varchar(50)` | not null |
| `email` | `varchar(255)` | not null, unique |
| `password_hash` | `varchar(255)` | not null |
| `department` | `varchar(100)` | NULL 허용 |
| `role` | `varchar(20)` | not null |
| `status` | `varchar(20)` | not null, default: 'PENDING' |
| `created_at` | `timestamp` | not null |
| `updated_at` | `timestamp` | NULL 허용 |
| `last_login_at` | `timestamp` | NULL 허용 |

### user_preferences

사용자별 기본 난이도와 자막 선호. 과거 세션 설정을 변경하지 않는다.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `user_id` | `bigint` | not null, unique |
| `default_difficulty` | `varchar(20)` | not null, default: 'INTERMEDIATE' |
| `default_subtitle_mode` | `varchar(30)` | not null, default: 'JAPANESE_KOREAN' |
| `created_at` | `timestamp` | not null |
| `updated_at` | `timestamp` | NULL 허용 |

### speaking_sessions

한 번의 학습과 소유자·모드·시작/종료·실제 이용시간. History와 통계의 기준.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `user_id` | `bigint` | not null |
| `session_type` | `varchar(20)` | not null |
| `status` | `varchar(20)` | not null, default: 'IN_PROGRESS' |
| `started_at` | `timestamp` | not null |
| `ended_at` | `timestamp` | NULL 허용 |
| `duration_seconds` | `int` | NULL 허용 |
| `created_at` | `timestamp` | not null |

### conversation_settings

일반회화 시작 당시 상황·상대방·성격·난이도·자막.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `session_id` | `bigint` | not null, unique |
| `situation` | `varchar(100)` | NULL 허용 |
| `partner_role` | `varchar(100)` | NULL 허용 |
| `partner_personality` | `varchar(100)` | NULL 허용 |
| `situation_description` | `text` | NULL 허용 |
| `difficulty` | `varchar(20)` | not null |
| `subtitle_mode` | `varchar(30)` | not null |
| `created_at` | `timestamp` | not null |

### interview_settings

면접 시작 당시 직무·유형·난이도·추가 요청과 언어 설정.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `session_id` | `bigint` | not null, unique |
| `job_role` | `varchar(100)` | not null |
| `interview_type` | `varchar(50)` | NULL 허용 |
| `difficulty` | `varchar(20)` | not null |
| `additional_request` | `text` | NULL 허용 |
| `subtitle_mode` | `varchar(30)` | not null |
| `target_country` | `varchar(30)` | default: 'JAPAN' |
| `target_language` | `varchar(30)` | default: 'JAPANESE' |
| `feedback_language` | `varchar(30)` | default: 'KOREAN' |
| `created_at` | `timestamp` | not null |

### speaking_messages

두 모드의 실제 사용자·AI 발화 기록(Transcript).

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `session_id` | `bigint` | not null |
| `sequence_no` | `int` | not null |
| `speaker` | `varchar(20)` | not null |
| `message_type` | `varchar(30)` | not null |
| `content` | `text` | not null |
| `created_at` | `timestamp` | not null |

### conversation_feedbacks

일반회화 전체 요약·자연스러움·문법·어휘·강점·다음 팁. 점수 없음.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `session_id` | `bigint` | not null, unique |
| `summary` | `text` | NULL 허용 |
| `naturalness_comment` | `text` | NULL 허용 |
| `grammar_comment` | `text` | NULL 허용 |
| `vocabulary_comment` | `text` | NULL 허용 |
| `strengths` | `json` | NULL 허용 |
| `next_tip` | `text` | NULL 허용 |
| `generation_status` | `varchar(20)` | not null, default: 'PENDING' |
| `generated_at` | `timestamp` | NULL 허용 |

### conversation_corrections

일반회화에서 교정할 표현별 원문·추천 표현·설명. 원본 메시지 연결은 선택.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `feedback_id` | `bigint` | not null |
| `message_id` | `bigint` | NULL 허용 |
| `category` | `varchar(30)` | not null |
| `original_expression` | `text` | not null |
| `suggested_expression` | `text` | not null |
| `explanation` | `text` | NULL 허용 |
| `display_order` | `int` | not null |

### interview_questions

기본질문·꼬리질문과 출제 상태·분석 결과. 부모 질문 및 생성 근거 답변 추적.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `session_id` | `bigint` | not null |
| `parent_question_id` | `bigint` | NULL 허용 |
| `source_answer_id` | `bigint` | NULL 허용 |
| `question_kind` | `varchar(30)` | not null |
| `question_text` | `text` | not null |
| `sequence_no` | `int` | NULL 허용 |
| `status` | `varchar(20)` | not null, default: 'SUGGESTED' |
| `intent` | `text` | NULL 허용 |
| `core_competencies` | `json` | NULL 허용 |
| `question_type` | `varchar(50)` | NULL 허용 |
| `star_recommended` | `boolean` | not null, default: false |
| `created_at` | `timestamp` | not null |

### interview_answers

질문별 답변 완료 시 확정된 평가 입력. MVP에서는 질문당 최종 답변 하나.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `question_id` | `bigint` | not null, unique |
| `answer_text` | `text` | not null |
| `submitted_at` | `timestamp` | not null |

### interview_answer_feedbacks

답변 하나의 Reflection 검토 완료 평가와 Coach의 개선 조언.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `answer_id` | `bigint` | not null, unique |
| `overall_score` | `decimal(5,2)` | NULL 허용 |
| `evaluation_summary` | `text` | NULL 허용 |
| `strengths` | `json` | NULL 허용 |
| `weaknesses` | `json` | NULL 허용 |
| `coaching_summary` | `text` | NULL 허용 |
| `improvement_tips` | `json` | NULL 허용 |
| `improved_answer` | `text` | NULL 허용 |
| `rubric_version` | `varchar(30)` | NULL 허용 |
| `generation_status` | `varchar(20)` | not null, default: 'PENDING' |
| `generated_at` | `timestamp` | NULL 허용 |

### interview_evaluation_scores

답변 평가 항목마다 한 행. 적용 여부·점수·항목 설명.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `answer_feedback_id` | `bigint` | not null |
| `criterion` | `varchar(50)` | not null |
| `score` | `decimal(5,2)` | NULL 허용 |
| `feedback` | `text` | NULL 허용 |
| `applicable` | `boolean` | not null, default: true |

### interview_feedbacks

면접 전체 종료 후 생성한 종합 점수·요약·강점·개선점.

| 컬럼 | 타입 | 제약·기본값 |
|---|---|---|
| `id` | `bigint` | pk, increment |
| `session_id` | `bigint` | not null, unique |
| `overall_score` | `decimal(5,2)` | NULL 허용 |
| `summary` | `text` | NULL 허용 |
| `strengths` | `json` | NULL 허용 |
| `improvements` | `json` | NULL 허용 |
| `evaluated_answer_count` | `int` | NULL 허용 |
| `rubric_version` | `varchar(30)` | NULL 허용 |
| `generation_status` | `varchar(20)` | not null, default: 'PENDING' |
| `generated_at` | `timestamp` | NULL 허용 |


## 3. 상태 및 데이터 규칙

| 구분 | 값 | 의미 |
|---|---|---|
| 기관 status | ACTIVE / INACTIVE | 운영·가입 선택 가능 / 비활성 |
| 사용자 role | LEARNER / MANAGER / ADMIN | 권한 구분 |
| 사용자 status | PENDING / ACTIVE / REJECTED / INACTIVE | 승인 대기 / 활성 / 거절 / 비활성 |
| session_type | CONVERSATION / INTERVIEW | 일반회화 / 면접 |
| 세션 status | IN_PROGRESS / COMPLETED / ABORTED | 진행 / 정상 종료 / 중도 종료 |
| generation_status | PENDING / PROCESSING / COMPLETED / FAILED | 생성 대기 / 생성 중 / 완료 / 실패 |
| difficulty | BEGINNER / INTERMEDIATE / ADVANCED | 초급 / 중급 / 고급 |
| subtitle_mode | OFF / JAPANESE / JAPANESE_KOREAN | 자막 없음 / 일본어 / 일본어+한국어 |
| speaker | USER / AI | 발화 주체 |
| message_type 예시 | QUESTION / ANSWER / FOLLOW_UP / CONVERSATION | 발화 종류 |
| question_kind | INITIAL / FOLLOW_UP | 기본질문 / 꼬리질문 |
| 질문 status | SUGGESTED / ASKED / ANSWERED / SKIPPED | 후보 / 실제 출제 / 답변됨 / 건너뜀 |
| 교정 category 예시 | GRAMMAR / VOCABULARY / NATURALNESS / EXPRESSION | 교정 구분 |

계정 전이는 `PENDING → ACTIVE`(승인), `PENDING → REJECTED`(거절), `ACTIVE → INACTIVE`(비활성), `INACTIVE → ACTIVE`(재활성)다. 거절 계정을 재신청시키는 정책은 아직 없다.

세션 정상 종료와 중도 종료 모두 ended_at을 기록하고 duration_seconds를 실제 시작·종료 시각으로 계산한다. 중도 종료는 종합 피드백을 새로 생성하지 않는다. 면접 도중 이미 저장된 답변별 평가를 삭제한다는 합의는 없다.

세션 COMPLETED와 피드백 generation_status의 COMPLETED는 서로 다른 상태다. 학습이 끝나도 AI가 처리 중이거나 실패할 수 있다. 생성 상태는 세 피드백 테이블에 존재한다.

면접 질문 후보 SUGGESTED를 실제 출제된 질문과 혼동하지 않는다. 실제 출제 시 ASKED, 답변 제출 시 ANSWERED로 처리한다. SKIPPED의 구체적 전이 조건은 구현 시 정한다.

## 4. 전체 API 목록

공개 API 외에는 JWT 인증이 필요하다. 아래 표가 최종 외부 API 목록이다.

| Method | Endpoint | 대상·기능 |
|---|---|---|
| POST | /api/auth/signup | 공개: Learner/Manager 가입 |
| POST | /api/auth/login | 공개: 공통 로그인 |
| GET | /api/auth/me | 로그인 상태·권한 |
| GET | /api/organizations/signup-options | 공개: ACTIVE 기관 선택 |
| GET | /api/users/me | 내 계정 |
| GET | /api/users/me/preferences | Learner 기본 설정 |
| PATCH | /api/users/me/preferences | Learner 기본 설정 변경 |
| PATCH | /api/users/me/password | 모든 역할 비밀번호 변경 |
| GET | /api/speaking/sessions/{sessionId} | 내 세션 공통 정보 |
| POST | /api/conversations | 일반회화 시작 |
| POST | /api/conversations/{sessionId}/turns | 사용자 발화·AI 응답 |
| POST | /api/conversations/{sessionId}/complete | 정상 종료·피드백 생성 |
| POST | /api/conversations/{sessionId}/abort | 중도 종료 |
| GET | /api/conversations/{sessionId}/feedback | 내 일반회화 피드백 |
| POST | /api/interviews | 면접 시작·첫 질문 |
| POST | /api/interviews/{sessionId}/questions/{questionId}/answer | 답변·평가·다음 질문 |
| POST | /api/interviews/{sessionId}/complete | 정상 종료·종합평가 |
| POST | /api/interviews/{sessionId}/abort | 중도 종료 |
| GET | /api/interviews/{sessionId}/feedback | 내 전체·답변별 평가 |
| GET | /api/history | 내 학습 목록 |
| GET | /api/history/{sessionId} | 내 학습 상세 |
| GET | /api/manager/dashboard | 기관 집계 |
| GET | /api/manager/learners | 같은 기관 학습자 |
| PATCH | /api/manager/learners/{userId}/approve | 가입 승인 |
| PATCH | /api/manager/learners/{userId}/reject | 가입 거절 |
| PATCH | /api/manager/learners/{userId}/deactivate | 비활성화 |
| PATCH | /api/manager/learners/{userId}/activate | 재활성화 |
| GET | /api/admin/dashboard | 서비스 전체 집계 |
| GET | /api/admin/organizations | 기관 목록 |
| POST | /api/admin/organizations | 기관 생성 |
| PATCH | /api/admin/organizations/{organizationId} | 기관명 수정 |
| PATCH | /api/admin/organizations/{organizationId}/deactivate | 기관 비활성화 |
| PATCH | /api/admin/organizations/{organizationId}/activate | 기관 재활성화 |
| GET | /api/admin/managers | Manager 목록 |
| PATCH | /api/admin/managers/{userId}/approve | Manager 승인 |
| PATCH | /api/admin/managers/{userId}/reject | Manager 거절 |
| PATCH | /api/admin/managers/{userId}/deactivate | Manager 비활성화 |
| PATCH | /api/admin/managers/{userId}/activate | Manager 재활성화 |

공통 세션 생성·complete·abort·messages는 별도 프론트용 API로 노출하지 않는다. 이전 대화의 공통 PATCH complete/abort 제안은 위 모드별 POST API로 대체됐다.

## 5. Auth / User 상세

### POST /api/auth/signup

Learner 요청:
```json
{
  "name": "김민지",
  "email": "minji@example.com",
  "password": "password123!",
  "role": "LEARNER",
  "organizationId": 1
}
```

Manager 요청:
```json
{
  "name": "김민수",
  "email": "minsu@company.com",
  "password": "password123!",
  "role": "MANAGER",
  "organizationId": 1,
  "department": "교육운영팀"
}
```

기존 기관 ID를 연결하고 status=PENDING으로 생성한다. 비밀번호는 password_hash에 해시로 저장한다. users.email은 UNIQUE다.

응답 예시:

```json
{
  "userId": 12,
  "role": "MANAGER",
  "status": "PENDING",
  "message": "가입 신청이 완료되었습니다. 관리자 승인 후 이용할 수 있습니다."
}
```


### GET /api/organizations/signup-options

인증 없이 ACTIVE 기관의 id, name을 조회한다. 최종 정책 설명에서 사용한 배열 예시:
```json
[
  {
    "id": 1,
    "name": "ABC 어학원"
  },
  {
    "id": 2,
    "name": "OO대학교 취업지원센터"
  }
]
```

초기 명세에는 `{"organizations":[...]}` 래퍼도 등장했다. 반환 데이터는 같지만 최종 DTO의 래퍼는 통일이 필요하다.

### POST /api/auth/login

요청:

```json
{
  "email": "minji@example.com",
  "password": "password123!"
}
```


응답 예시:

```json
{
  "accessToken": "jwt-token",
  "user": {
    "id": 15,
    "name": "김민지",
    "email": "minji@example.com",
    "role": "LEARNER",
    "status": "ACTIVE",
    "organization": {
      "id": 1,
      "name": "ABC 어학원"
    }
  }
}
```


역할별 화면 이동: LEARNER → /dashboard, MANAGER → /manager/dashboard, ADMIN → /admin/dashboard. 화면 이동만으로 서버 권한 검사를 대신하지 않는다.

대기·거절·비활성 오류 예시는 각각 ACCOUNT_PENDING, ACCOUNT_REJECTED, ACCOUNT_INACTIVE다. 대화에서는 403을 제안했으나 전체 HTTP 오류 매핑은 아직 최종 확정하지 않았다.

대기 오류 예시:

```json
{
  "code": "ACCOUNT_PENDING",
  "message": "현재 가입 승인 대기 중입니다."
}
```


### GET /api/auth/me

`Authorization: Bearer {accessToken}`으로 현재 로그인 상태와 권한을 복원한다.

응답 예시:

```json
{
  "id": 15,
  "name": "김민지",
  "email": "minji@example.com",
  "role": "LEARNER",
  "status": "ACTIVE",
  "organization": {
    "id": 1,
    "name": "ABC 어학원"
  }
}
```


### GET /api/users/me

Settings에서 사용할 계정 정보. 기본 응답은 위 /auth/me와 같은 id, name, email, role, status, organization이다. Manager는 department도 반환하는 방향이다. Admin의 기관은 NULL이다.

### GET /api/users/me/preferences

응답 예시:

```json
{
  "defaultDifficulty": "INTERMEDIATE",
  "defaultSubtitleMode": "JAPANESE_KOREAN"
}
```


### PATCH /api/users/me/preferences

요청:

```json
{
  "defaultDifficulty": "ADVANCED",
  "defaultSubtitleMode": "JAPANESE"
}
```


응답 예시:

```json
{
  "defaultDifficulty": "ADVANCED",
  "defaultSubtitleMode": "JAPANESE",
  "message": "설정이 저장되었습니다."
}
```


user_preferences만 변경하며 이미 생성된 세션 settings를 수정하지 않는다.

### PATCH /api/users/me/password

요청:

```json
{
  "currentPassword": "oldPassword123!",
  "newPassword": "newPassword123!"
}
```


응답 예시:

```json
{
  "message": "비밀번호가 변경되었습니다."
}
```


현재 비밀번호 불일치 오류:

```json
{
  "code": "INVALID_CURRENT_PASSWORD",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```


## 6. Speaking / Conversation / Interview 상세

### GET /api/speaking/sessions/{sessionId}

본인 세션만 조회한다.
```json
{
  "sessionId": 201,
  "sessionType": "INTERVIEW",
  "status": "IN_PROGRESS",
  "startedAt": "2026-09-09T09:30:00",
  "endedAt": null,
  "durationSeconds": null
}
```


### POST /api/conversations

요청:

```json
{
  "situation": "CAFE",
  "partnerRole": "친구",
  "partnerPersonality": "밝고 친근함",
  "situationDescription": "오랜만에 만난 친구와 카페에서 대화",
  "difficulty": "INTERMEDIATE",
  "subtitleMode": "JAPANESE_KOREAN"
}
```


한 요청에서 본인의 speaking_sessions(CONVERSATION, IN_PROGRESS)와 conversation_settings를 생성한다.

응답 예시:

```json
{
  "sessionId": 101,
  "sessionType": "CONVERSATION",
  "status": "IN_PROGRESS",
  "startedAt": "2026-09-09T10:10:00"
}
```


### POST /api/conversations/{sessionId}/turns

요청:

```json
{
  "userMessage": "久しぶり！最近どう？"
}
```


사용자 메시지 저장 → settings·최근 대화·현재 발화를 AI에 전달 → 일본어 응답 생성 → AI 메시지 저장. 두 메시지 모두 speaking_messages에 저장하고 speaker는 서버가 지정한다.

응답 예시:

```json
{
  "userMessage": {
    "messageId": 301,
    "content": "久しぶり！最近どう？"
  },
  "aiMessage": {
    "messageId": 302,
    "content": "久しぶり！元気だったよ。最近は仕事がちょっと忙しくてね。"
  }
}
```


음성 연동은 `음성 → STT → userMessage → AI → aiMessage → TTS → 재생` 흐름이다. 현재 계약은 텍스트 기준이다.

### POST /api/conversations/{sessionId}/complete

별도 입력 필드는 정의되지 않았다. 세션을 COMPLETED로 변경하고 종료 시각·이용시간을 저장한다. 전체 메시지를 바탕으로 AI 피드백을 생성해 conversation_feedbacks와 conversation_corrections에 저장한다. complete 자체의 정확한 응답 DTO는 미정이다.

### POST /api/conversations/{sessionId}/abort

세션을 ABORTED로 변경하고 종료 시각·이용시간을 저장한다. 피드백 생성 없이 Setup으로 돌아간다. 별도 요청 필드는 정의되지 않았다. 응답의 핵심 의미는 sessionId와 ABORTED이며 최종 DTO는 미정이다.

### GET /api/conversations/{sessionId}/feedback

일반회화는 점수 없이 전체 요약과 표현 교정을 반환한다.

응답 예시:

```json
{
  "sessionId": 101,
  "summary": "전체적으로 자연스럽게 대화를 이어갔습니다.",

  "naturalnessComment": "...",
  "grammarComment": "...",
  "vocabularyComment": "...",

  "strengths": [
    "...",
    "..."
  ],

  "nextTip": "...",

  "corrections": [
    {
      "messageId": 305,
      "category": "NATURALNESS",
      "originalExpression": "...",
      "suggestedExpression": "...",
      "explanation": "..."
    }
  ]
}
```



### POST /api/interviews

요청:

```json
{
  "jobRole": "백엔드 개발자",
  "interviewType": "TECHNICAL",
  "difficulty": "INTERMEDIATE",
  "additionalRequest": "신입 개발자 수준으로 진행해주세요.",
  "subtitleMode": "JAPANESE_KOREAN"
}
```


서버에서 target_country=JAPAN, target_language=JAPANESE, feedback_language=KOREAN으로 설정한다. 세션(INTERVIEW, IN_PROGRESS)·면접 settings 생성 후 첫 질문을 생성·저장한다.

응답 예시:

```json
{
  "sessionId": 201,
  "status": "IN_PROGRESS",
  "firstQuestion": {
    "questionId": 501,
    "questionKind": "INITIAL",
    "questionText": "自己紹介をお願いします。",
    "sequenceNo": 1
  }
}
```


### POST /api/interviews/{sessionId}/questions/{questionId}/answer

요청:

```json
{
  "answerText": "私は大学でソフトウェアを専攻し..."
}
```


본인 면접 세션에 속한 질문인지 확인하는 구조다. 확정 답변을 저장하고 질문을 ANSWERED로 변경한다. Question Analyzer → Answer Evaluator → Reflection → Interview Coach를 거쳐 최종 평가·코칭을 저장하고 다음 질문을 결정한다.

꼬리질문이면 question_kind=FOLLOW_UP, parent_question_id=원래 질문 ID, source_answer_id=방금 답변 ID, status=ASKED로 저장한다. 새 기본질문이면 INITIAL이다.

응답 예시:

```json
{
  "answerId": 601,
  "nextQuestion": {
    "questionId": 502,
    "questionKind": "FOLLOW_UP",
    "questionText": "その意見の違いをどのように調整しましたか？",
    "sequenceNo": 2
  },
  "isComplete": false
}
```


마지막 답변 응답:

```json
{
  "answerId": 610,
  "nextQuestion": null,
  "isComplete": true
}
```


isComplete는 다음 질문 유무에 대한 서버 판단이다. 마지막 답변 후 complete API를 호출하므로 isComplete=true만으로 종합평가 저장까지 완료됐다고 해석하지 않는다.

### POST /api/interviews/{sessionId}/complete

별도 입력 필드는 정의되지 않았다. 세션 정상 종료·시간 기록 후 모든 질문, 확정 답변, 답변별 최종 평가를 입력으로 면접 전체 종합평가를 생성하여 interview_feedbacks에 저장한다. complete 응답 DTO는 미정이다.

### POST /api/interviews/{sessionId}/abort

ABORTED와 종료 시각·이용시간을 기록한다. 종합 Feedback을 새로 생성하지 않는다. 면접 뒤로가기 → 종료 확인 → abort 호출 → /interview/setup 흐름이다. 최종 응답 DTO는 미정이다.

### GET /api/interviews/{sessionId}/feedback

전체 평가와 답변별 평가를 한 번에 조회한다. 아래는 대화에서 합의한 응답 개념 예시이며 생성 중·실패 응답은 아직 구체화하지 않았다.

응답 예시:

```json
{
  "sessionId": 201,
  "overall": {
    "overallScore": 81,
    "summary": "...",
    "strengths": ["..."],
    "improvements": ["..."]
  },

  "answers": [
    {
      "question": "自己紹介をお願いします。",
      "answer": "私は大学で...",
      "overallScore": 82,

      "scores": [
        {
          "criterion": "COMMUNICATION",
          "score": 85,
          "feedback": "...",
          "applicable": true
        }
      ],

      "strengths": ["..."],
      "weaknesses": ["..."],
      "coachingSummary": "...",
      "improvementTips": ["..."],
      "improvedAnswer": "..."
    }
  ]
}
```



## 7. History / Manager / Admin 상세

### GET /api/history

본인의 기록. 선택 필터는 `?type=CONVERSATION`, `?type=INTERVIEW`다.

응답 예시:

```json
{
  "items": [
    {
      "sessionId": 201,
      "sessionType": "INTERVIEW",
      "status": "COMPLETED",
      "title": "백엔드 개발자 면접",
      "startedAt": "2026-09-09T09:30:00",
      "durationSeconds": 720,
      "overallScore": 81
    },
    {
      "sessionId": 101,
      "sessionType": "CONVERSATION",
      "status": "COMPLETED",
      "title": "카페에서 친구와 대화",
      "startedAt": "2026-09-08T15:20:00",
      "durationSeconds": 530,
      "overallScore": null
    }
  ]
}
```


title은 일반회화 situation, 면접 job_role 등으로 조합하며 새 DB 컬럼이 필요 없다. 일반회화 overallScore는 null이다.

### GET /api/history/{sessionId}

sessionType에 따라 해당 세션·settings·Transcript·피드백을 조합한다. 일반회화는 요약·교정, 면접은 전체·답변별 평가를 반환한다.

응답 예시:

```json
{
  "sessionId": 101,
  "sessionType": "CONVERSATION",
  "startedAt": "...",
  "durationSeconds": 530,
  "settings": {
    "situation": "CAFE",
    "difficulty": "INTERMEDIATE"
  },
  "feedback": {
    "summary": "...",
    "naturalnessComment": "...",
    "grammarComment": "...",
    "vocabularyComment": "...",
    "strengths": ["..."],
    "nextTip": "...",
    "corrections": []
  }
}
```


이 예시에는 Transcript 배열이 생략돼 있다. Transcript는 speaking_messages를 sequence_no 순으로 읽지만 상세 응답의 정확한 배열 필드명은 미정이다. 프론트 /history/:id는 sessionType으로 화면을 분기한다.

### GET /api/manager/dashboard

Manager 본인 기관으로 집계 범위를 제한한다. 별도 통계 테이블 없이 users와 speaking_sessions에서 계산한다.

응답 예시:

```json
{
  "learnerCount": 42,
  "monthlySpeakingCount": 128,
  "averageStudyMinutes": 18.5,
  "monthlyInterviewCount": 37,
  "pendingLearnerCount": 3,
  "weeklyUsage": [
    {
      "date": "2026-09-07",
      "studyMinutes": 420
    },
    {
      "date": "2026-09-08",
      "studyMinutes": 510
    }
  ]
}
```


### GET /api/manager/learners

필터: status=PENDING/ACTIVE/INACTIVE, keyword=이름 검색. 같은 기관의 LEARNER만 대상이다.

응답 예시:

```json
{
  "items": [
    {
      "userId": 15,
      "name": "김민지",
      "email": "minji@example.com",
      "status": "ACTIVE",
      "createdAt": "2026-08-10T10:00:00",
      "studyCount": 12,
      "lastActivityAt": "2026-09-08T19:20:00"
    }
  ]
}
```


### PATCH /api/manager/learners/{userId}/approve
### PATCH /api/manager/learners/{userId}/reject
### PATCH /api/manager/learners/{userId}/deactivate
### PATCH /api/manager/learners/{userId}/activate

동일 기관·학습자 역할을 확인한 후 각각 PENDING→ACTIVE, PENDING→REJECTED, ACTIVE→INACTIVE, INACTIVE→ACTIVE로 변경한다. 별도 요청 body는 정의되지 않았다.

approve 응답:

```json
{
  "userId": 15,
  "status": "ACTIVE",
  "message": "학습자 가입이 승인되었습니다."
}
```


나머지 동작은 상태 전이가 확정돼 있고 응답 DTO·메시지 문구는 미정이다. 상세 답변·AI 피드백을 응답에 추가하지 않는다.

### GET /api/admin/dashboard

응답 예시:

```json
{
  "organizationCount": 12,
  "userCount": 520,
  "monthlySpeakingCount": 1840,
  "monthlyActiveUsers": 310,
  "pendingManagerCount": 4,
  "weeklyUsage": [
    {
      "date": "2026-09-07",
      "sessionCount": 250
    }
  ]
}
```


전체 기관·사용자·세션 데이터를 집계한다.

### GET /api/admin/organizations

필터 예: `?status=ACTIVE&keyword=대학교`.

응답 예시:

```json
{
  "items": [
    {
      "organizationId": 1,
      "name": "OO대학교 취업지원센터",
      "status": "ACTIVE",
      "managerCount": 2,
      "learnerCount": 75,
      "createdAt": "2026-08-01T10:00:00"
    }
  ]
}
```


### POST /api/admin/organizations

요청:

```json
{
  "name": "OO대학교 취업지원센터"
}
```


기관은 ACTIVE로 생성되며 즉시 가입 선택 목록에 포함된다.

응답 예시:

```json
{
  "organizationId": 13,
  "name": "OO대학교 취업지원센터",
  "status": "ACTIVE"
}
```


### PATCH /api/admin/organizations/{organizationId}

요청:

```json
{
  "name": "OO대학교 커리어센터"
}
```


기관명 수정. 응답 DTO는 미정이다.

### PATCH /api/admin/organizations/{organizationId}/deactivate
### PATCH /api/admin/organizations/{organizationId}/activate

ACTIVE→INACTIVE / INACTIVE→ACTIVE. 실제 행을 삭제하지 않는다. 비활성 기관은 가입 목록에서 제외된다. 별도 body·최종 응답 DTO는 정의되지 않았다.

### GET /api/admin/managers

필터 예: status=PENDING, status=ACTIVE, organizationId=1.

응답 예시:

```json
{
  "items": [
    {
      "userId": 30,
      "name": "김민수",
      "email": "minsu@company.com",
      "department": "교육운영팀",
      "organization": {
        "id": 1,
        "name": "ABC 어학원"
      },
      "status": "PENDING",
      "createdAt": "...",
      "lastLoginAt": null
    }
  ]
}
```


### PATCH /api/admin/managers/{userId}/approve
### PATCH /api/admin/managers/{userId}/reject
### PATCH /api/admin/managers/{userId}/deactivate
### PATCH /api/admin/managers/{userId}/activate

Admin이 MANAGER 계정을 대상으로 각각 PENDING→ACTIVE, PENDING→REJECTED, ACTIVE→INACTIVE, INACTIVE→ACTIVE를 적용한다. 별도 요청 body·응답 DTO는 정의되지 않았다.

## 8. AI 출력 계약과 DB 매핑

### 일반회화 피드백 출력

AI 출력:

```json
{
  "summary": "전체적으로 자연스럽게 대화를 이어갔습니다.",
  "naturalnessComment": "문맥에 맞는 표현을 잘 사용했습니다.",
  "grammarComment": "큰 문법 오류는 없었습니다.",
  "vocabularyComment": "일상적인 어휘를 적절하게 사용했습니다.",

  "strengths": [
    "대화 흐름을 자연스럽게 이어감",
    "상황에 맞는 표현을 사용함"
  ],

  "nextTip": "조사 사용과 자연스러운 생략 표현을 조금 더 연습해보세요.",

  "corrections": [
    {
      "messageId": 305,
      "category": "NATURALNESS",
      "originalExpression": "私はコーヒーを飲みたいです。",
      "suggestedExpression": "コーヒー飲みたいな。",
      "explanation": "친구와의 일상적인 대화에서는 주어를 생략하고 조금 더 캐주얼하게 표현할 수 있습니다."
    }
  ]
}
```


| JSON | DB |
|---|---|
| summary, naturalnessComment, grammarComment, vocabularyComment, strengths, nextTip | conversation_feedbacks의 대응 snake_case 컬럼 |
| corrections 각 원소 | conversation_corrections 한 행 |
| corrections[].messageId | message_id → speaking_messages.id |
| 교정 표시 순서 | display_order |

### 면접 답변별 출력과 저장

질문 분석의 intent, coreCompetencies, questionType, starRecommended는 interview_questions에 대응한다. 검토된 overallScore, evaluationSummary, strengths, weaknesses와 코칭의 coachingSummary, improvementTips, improvedAnswer는 interview_answer_feedbacks에 저장한다. scores 각 원소는 interview_evaluation_scores 한 행이다.

평가 항목: LOGICAL_THINKING, SPECIFICITY, COMMUNICATION, JOB_FIT, STAR_STRUCTURE 및 후속 API 예시에 포함된 QUESTION_RELEVANCE, BUSINESS_JAPANESE. DBML에서는 마지막 두 항목을 신규 후보로 기재했으므로, 새 rubric의 정확한 계산·가중치는 아직 확정되지 않았다.

평가 대상이 아닌 항목:

저장 의미:

```json
{
  "criterion": "STAR_STRUCTURE",
  "score": null,
  "applicable": false
}
```


0점은 평가 결과가 낮다는 의미다. 적용 불가 항목을 0점으로 대체하지 않는다. rubric_version은 어떤 기준으로 평가했는지를 남긴다.

### 면접 전체 출력

AI 출력:

```json
{
  "overallScore": 81,
  "summary": "질문의 의도를 전반적으로 잘 이해하고 답변했습니다...",
  "strengths": [
    "경험을 구체적으로 설명함",
    "질문에 직접적으로 답변함"
  ],
  "improvements": [
    "결론을 먼저 제시하면 전달력이 좋아집니다.",
    "비즈니스 일본어 표현을 보완할 필요가 있습니다."
  ],
  "evaluatedAnswerCount": 10,
  "rubricVersion": "v1"
}
```


interview_feedbacks의 overall_score, summary, strengths, improvements, evaluated_answer_count, rubric_version에 매핑한다. generated_at과 generation_status는 생성 과정에서 서버가 관리한다.

AI 출력에 맞춰 임의 컬럼을 계속 추가하는 방식이 아니라, 합의된 구조를 출력 계약으로 두고 서버가 파싱·검증·매핑한다. Evaluator 중간 원본을 정식 평가로 중복 저장하지 않는다. 일본어 개선 답변은 improved_answer 하나를 사용한다.

## 9. 미정 사항 및 프론트 수정

다음 항목은 기존 합의를 바꾸지 않고 구현 단계에서 구체화할 부분이다.

| 항목 | 현재 기준 / 남은 결정 |
|---|---|
| 질문 수 | 최대 10개·꼬리질문 1~2개는 예시일 뿐 미확정. 무한 질문을 막는 제한 필요 |
| 점수 정책 | rubric_version 사용. 점수 범위 검증·평균·가중치·반올림 및 전체 점수 산식 미확정 |
| AI 생성 상태 | DB의 4개 상태는 존재. 조회 중/실패 응답, 비동기 처리·재시도 API/방식은 미정 |
| API 공통 계약 | 성공 HTTP 코드, 전체 오류 코드, 페이징·정렬, 시간대 표기, 필드별 필수/길이 검증, 중복 요청 처리 미정 |
| 인증 | JWT 사용. 만료·갱신·로그아웃 계약은 이 대화에서 별도 명세하지 않음 |
| 기관 비활성 영향 | 가입 목록 제외 확정. 기존 소속 사용자 자동 비활성·진행 세션 처리까지 합의한 것은 아님 |
| History | 정상 완료는 Feedback 대상. ABORTED/진행 세션 목록 노출·필터 규칙은 구체화 필요 |
| 통계 | sessions 집계 확정. 완료/중도 종료 포함 범위, 월간 기준 시간대, 평균 분모·활성이용자 정의 미정 |
| Learner Dashboard | sessions 기반 방향은 있음. 별도 전용 엔드포인트는 최종 API 목록에 없음 |
| 음성·자막 | STT/TTS 및 번역 전달·저장·스트리밍 계약 미정. translated_text 컬럼은 최신 ERD에 없음 |
| JSON 저장 | 최신 ERD는 json. DBMS에 따른 jsonb 등 물리 구현은 추후 결정 |
| 데이터 무결성 | 중복 답변·종료 요청, 세션/질문/교정 메시지 소속 일치, 실패 시 저장 경계 등 구현 검증 필요 |

### 프론트-ERD 대조

- 기관 등록 UI의 담당자 이메일·계약 시작일은 organizations에 저장할 컬럼이 없다. MVP 기관 화면은 기관명·상태에 맞춘다. 생성 API는 이름만 받고 ACTIVE를 서버에서 설정한다.
- Manager는 기관 등록과 별개로 가입하고 Admin 승인을 받는다. 기관 생성 시 Manager 계정이 함께 생성되지 않는다.
- Manager 가입 화면의 기관명 자유 입력은 기존 기관 Select Box로 수정한다. Learner도 같은 목록을 사용한다.
- 기본 난이도·자막은 user_preferences API에 연결한다. 이번 학습 설정은 각 시작 API로 보낸다.
- 면접 종료 판단은 프론트 임시 question index 대신 답변 응답의 isComplete를 따른다.
- 일반회화 점수 UI가 남아 있다면 무점수 피드백 구조와 맞춘다.
- 기존 Agent의 5개 평가 항목과 화면의 질문 적합성·비즈니스 일본어를 새 rubric에 맞추는 작업이 남아 있다.
- 전체 transcript는 메시지에서 조합하며 피드백 테이블에 통째로 중복 저장하는 컬럼을 추가하지 않는다.

학습용 이유와 데이터 흐름은 [설계 해설 문서](ai-japanese-speaking-design-study-notes.md)를 참고한다.

