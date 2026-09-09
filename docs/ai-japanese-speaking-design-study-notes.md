# AI 일본어 Speaking 서비스: 설계 복습 노트

기준일: 2026-09-09  
근거: [웹 프로젝트 기획](chatgpt-conversation://6a99632e-1628-83ee-a66d-e376c3dc74ff)의 최신 합의 및 14개 테이블 ERD. 정확한 컬럼·엔드포인트는 [ERD + API 기준 문서](ai-japanese-speaking-erd-api-spec.md)를 함께 본다.

이 문서는 “왜 이렇게 나눴는가”를 이해하기 위한 해설이다. 예시 데이터는 학습용이며 새 기능이나 정책을 추가하지 않는다. 구현 때 검토할 내용은 확정사항과 구분했다.

## 1. 화면 하나가 곧 테이블 하나는 아니다

화면은 사용자가 보기 좋은 형태로 정보를 묶고, 테이블은 저장할 사실과 관계를 나눈다. API는 그 사이에서 화면에 필요한 정보를 조합하거나 사용자의 동작을 처리한다.

예를 들어 History 화면에는 제목, 학습 시간, 면접 점수가 함께 보인다. 하지만 제목은 settings에서 만들고, 시간은 speaking_sessions에서 읽고, 점수는 interview_feedbacks에서 읽는다. 한 화면에 함께 보인다고 한 테이블에 모두 넣을 필요는 없다.

반대로 면접 피드백 화면 하나를 위해 여러 테이블을 읽을 수 있다. 서버가 이를 합쳐 한 응답으로 주면 프론트는 한 API를 호출해서 화면을 그릴 수 있다.

```text
사용자의 행동: 면접 답변 완료
  → API: 답변 제출
  → DB: 답변 저장
  → AI: 분석·평가·검토·코칭
  → DB: 평가·점수·다음 질문 저장
  → 응답: nextQuestion, isComplete
```

따라서 “테이블이 14개면 API도 14개인가?”에 대한 답은 아니다. 테이블은 데이터의 구조, API는 사용할 수 있는 동작의 경계다.

## 2. organizations와 users: 기관과 계정을 나누는 이유

기관에는 이름과 운영 상태가 있고, 사용자에는 이메일·비밀번호 해시·역할·가입 상태가 있다. 기관 하나에 Manager 여러 명과 Learner 여러 명이 소속될 수 있으므로 기관 정보를 사용자마다 반복 저장하지 않는다.

예를 들어 같은 기관 이름을 사용자 75명의 행에 복사하면 기관명 변경 시 75행을 고쳐야 한다. organizations에 한 행을 두고 users.organization_id로 연결하면 기관명은 한 곳에서 바꾼다.

### 승인 체인은 계정 역할에 따라 다르다

```text
Admin이 기관 등록
  → Manager가 기존 기관 선택 후 가입
  → Admin이 Manager 승인
  → Learner가 기존 기관 선택 후 가입
  → 해당 기관 Manager가 Learner 승인
```

Admin은 기관과 서비스를 운영하고, Manager는 자신이 담당하는 기관의 학습자를 관리한다. 이 책임 범위가 승인 권한에도 이어진다. Manager의 역할만 확인해서는 부족하며 대상 Learner와 organization_id가 같은지도 확인해야 한다.

초기 대화에 있던 “Manager 가입과 동시에 기관 생성” 제안은 최종 정책이 아니다. 사용자가 Admin 선등록 + Select Box 방식을 선택했기 때문에 가입 API는 organizationId를 받아 기존 기관에 연결한다.

### Admin도 users에 저장하는 이유

Admin도 이메일·비밀번호·로그인 상태를 가진 계정이다. 공통 users에 role=ADMIN으로 저장하면 공통 로그인 구조를 사용할 수 있다. 특정 기관을 담당하는 계정이 아니므로 organization_id는 NULL이다.

공개 회원가입에서 ADMIN을 허용하는 것은 별개 문제다. 서버는 가입 역할을 LEARNER/MANAGER로 제한하고 Admin은 DB 초기 데이터로 만든다. password_hash에는 평문 비밀번호가 아니라 해시값을 넣는다. 관리자 회원가입 버튼을 숨기는 것만으로 서버의 생성 권한이 제한되지는 않는다.

## 3. user_preferences와 세션 settings: 기본값과 과거 사실

user_preferences는 “다음 학습에서 우선 보여줄 값”이다. conversation_settings와 interview_settings는 “그 학습을 시작할 때 실제 선택한 값”이다.

| 상황 | 저장 위치 | 값 |
|---|---|---|
| 평소 중급을 선호 | user_preferences.default_difficulty | INTERMEDIATE |
| 오늘 면접만 고급으로 시작 | interview_settings.difficulty | ADVANCED |
| 내일부터 초급으로 기본값 변경 | user_preferences.default_difficulty | BEGINNER |
| 오늘 면접을 History에서 다시 열기 | 기존 interview_settings.difficulty | 계속 ADVANCED |

과거 면접이 갑자기 초급 연습으로 표시되면 기록이 틀어진다. 그래서 시작 당시 값을 별도로 남긴다. 이는 단순 중복이 아니라 서로 다른 시점의 사실을 저장하는 것이다.

user_preferences.user_id에 UNIQUE가 있으므로 사용자당 기본 설정은 최대 한 행이다. 각 settings의 session_id에도 UNIQUE가 있으므로 같은 세션의 설정이 여러 행으로 늘어나지 않는다.

기본 설정 API가 세션 settings까지 수정하는 것은 현재 합의가 아니다. Settings 화면에서는 기본값을 바꾸고, Setup에서는 이번 학습 값을 선택하여 시작 요청에 보낸다.

## 4. speaking_sessions가 중심인 이유

두 모드는 내용과 평가 방식은 다르지만 공통으로 “누가, 언제, 어떤 모드로, 얼마나 학습했는가”를 가진다. 이 공통 사실이 speaking_sessions다.

```text
users
  └─ speaking_sessions
       ├─ speaking_messages
       ├─ 일반회화: conversation_settings
       │             └─ 세션 전체 feedback + corrections
       └─ 면접: interview_settings
                  ├─ questions → answers → answer_feedbacks → scores
                  └─ 세션 전체 interview_feedbacks
```

위 그림은 데이터 흐름을 설명한 것이다. 실제 FK는 feedback과 questions가 settings를 거치지 않고 session_id로 세션을 참조한다.

일반회화 한 번과 면접 한 번을 각각 하나의 세션으로 다루면 History와 Dashboard에서 두 모드의 이용량을 함께 셀 수 있다. 시작·종료 시간 계산을 모드마다 다른 테이블에서 찾아 합칠 필요도 줄어든다.

다만 세션의 공통성이 모든 데이터를 한 테이블에 넣어야 한다는 뜻은 아니다. 일반회화의 상대방 성격과 면접의 지원 직무는 각각 settings에 둔다.

## 5. History 전용 테이블이 필요 없는 이유

History는 새 사건이 아니라 이미 끝난 학습을 다시 읽는 기능이다.

예를 들어 sessionId=201을 조회하면 다음 데이터를 연결한다.

```text
speaking_sessions 201 → 면접, 시작 시각, 이용시간
interview_settings(session_id=201) → 백엔드 개발자
interview_feedbacks(session_id=201) → 전체 점수·요약
interview_questions → interview_answers → 답변별 평가
speaking_messages(session_id=201) → 실제 발화 순서
```

“백엔드 개발자 면접”이라는 제목도 job_role을 바탕으로 만들 수 있다. title을 위한 컬럼이나 History 테이블을 만들지 않아도 된다.

별도 History에 시간·점수를 복사하면 피드백 생성 후 원본 점수만 저장되고 History 복사본은 비어 있는 불일치가 생길 수 있다. 현재 MVP는 원본을 조합해 읽는 방식으로 이 문제를 줄인다.

일반회화 overallScore=null은 점수 없이 피드백을 제공하기 때문이다. 0점을 받았다는 뜻이 아니다. ABORTED 세션을 목록에 어떤 방식으로 노출할지는 아직 상세 합의가 없으므로 모든 기록이 반드시 표시된다고 가정하지 않는다.

## 6. 일반회화와 면접 API를 분리한 이유

일반회화의 중심 동작은 발화 한 번을 보내고 AI 응답을 받는 것이다. 면접의 중심 동작은 특정 질문에 답변을 확정하고 평가한 뒤 다음 질문을 결정하는 것이다.

| 일반회화 | 면접 |
|---|---|
| 상황·대화 상대·성격 | 지원 직무·면접 유형 |
| userMessage | questionId에 대한 answerText |
| 대화 흐름을 이어갈 AI 응답 | 평가를 거친 다음/꼬리질문 |
| 종료 후 가벼운 표현 피드백 | 답변별 평가와 전체 종합평가 |

그래서 /api/conversations/{sessionId}/turns와 /api/interviews/{sessionId}/questions/{questionId}/answer가 나뉜다. 두 요청을 단순 “메시지 저장”으로 묶으면 면접의 질문-답변 연결과 평가 동작이 API 밖으로 흩어진다.

시작 API도 세션 생성과 설정 저장을 한 동작으로 묶는다. Setup의 시작 버튼 한 번으로 /api/conversations 또는 /api/interviews를 호출한다. 세션만 먼저 만들고 프론트가 다음 요청으로 설정을 붙이는 외부 흐름은 채택하지 않았다.

## 7. complete와 abort를 모드별로 둔 이유

complete는 상태만 바꾸는 동작이 아니다. 일반회화에서는 전체 대화로 요약과 교정을 만들고, 면접에서는 질문·답변·최종 평가를 모아 종합평가를 만든다.

```text
일반회화 complete
  → 종료 시간·이용시간
  → 전체 메시지
  → conversation_feedbacks + conversation_corrections

면접 complete
  → 종료 시간·이용시간
  → 모든 질문·답변·답변별 최종 평가
  → interview_feedbacks
```

이 차이를 모드별 종료 API가 담당한다. 내부에서 공통 시간 계산 로직을 재사용하는 것은 가능하지만 프론트에 별도 공통 종료 API를 노출하는 것과는 다르다.

abort는 사용자가 중간에 그만둔 사실을 남긴다. 이용시간은 기록하지만 정상 완료 피드백을 새로 만들지 않는다. 면접 도중 생성된 답변별 평가가 있다면 그것을 삭제한다는 정책은 현재 없다.

세션 COMPLETED와 피드백 생성 COMPLETED도 다르다. 사용자의 학습은 끝났지만 AI는 아직 PROCESSING이거나 FAILED일 수 있다. 그래서 generation_status를 별도로 둔다. 생성 중 화면·재시도 통신 방식은 앞으로 구체화해야 한다.

## 8. userId를 JWT에서 얻는 이유

학습자가 요청 body에 userId=20을 넣었다고 해서 서버가 20번 사용자의 세션을 만들어 주면, 로그인한 사람이 다른 사람의 이름으로 데이터를 만들 수 있다.

현재 구조에서는 검증된 JWT의 인증 주체로 사용자를 식별한다.

```text
로그인 → accessToken
학습 시작 요청 + Bearer 토큰
  → 서버가 토큰 검증
  → 로그인 사용자 ID 확인
  → speaking_sessions.user_id에 저장
```

세션 조회에서도 sessionId를 받는 것과 소유권이 확인되는 것은 다르다. 서버는 그 세션이 로그인 사용자의 것인지 확인해야 한다.

반면 Manager 승인 URL의 userId는 “승인 대상”이다. 이 값은 URL로 전달하되, 요청자가 Manager인지, 대상이 같은 기관의 Learner인지 확인한다. 인증 주체와 작업 대상 ID의 역할을 구분하면 이해하기 쉽다.

## 9. speaking_messages, interview_questions, interview_answers의 차이

세 테이블 모두 문장을 저장하지만 의미가 다르다.

| 테이블 | 답하는 질문 |
|---|---|
| speaking_messages | 실제로 어떤 순서로 누가 무엇을 말했는가? |
| interview_questions | 어떤 질문을 출제했고, 의도와 부모 질문은 무엇인가? |
| interview_answers | 이 질문에 대해 최종 평가한 답변은 무엇인가? |

STT가 “졸업 프로젝트에서…”, “팀원과 의견을 조율했고…”, “결과적으로…”처럼 여러 조각을 만들 수 있다. Transcript에는 발화 흐름을 남기고 답변 완료 시 평가 대상 텍스트를 확정하여 interview_answers에 저장한다. 실제 STT 조각 저장 단위와 전달 방식은 아직 미정이다.

같은 문장이 messages와 answers에 존재할 수 있지만 전자는 대화 기록, 후자는 평가 입력이라는 목적이 있다. 이 중복을 없애려고 answers를 삭제하면 질문별 최종 답변을 식별하는 기준이 약해진다.

현재 ERD에는 speaking_messages.interview_answer_id가 없다. 초기 분석에서 제안된 연결 컬럼을 최신 확정 구조에 몰래 추가하면 안 된다. 지금은 메시지는 세션에 연결하고 답변은 질문에 연결한다.

MVP의 question_id UNIQUE는 질문당 최종 답변 하나라는 제약이다. 여러 번 다시 답하는 기능을 추가하려면 시도별 저장 설계를 별도로 논의해야 한다.

## 10. 꼬리질문은 왜 부모 질문과 근거 답변을 모두 가질까?

예를 들어 Q1이 “협업 과정에서 어려움을 해결한 경험”을 묻고, A1이 “의견 차이를 조율했다”고만 답했다면 Q2는 “어떻게 조율했는가”를 물을 수 있다.

```text
Q1(id=501) → A1(id=601)
                  ↓
Q2(id=502, FOLLOW_UP)
  parent_question_id = 501
  source_answer_id = 601
```

parent_question_id는 어느 질문에서 이어졌는지, source_answer_id는 어느 답변의 내용을 근거로 생성됐는지 설명한다. 두 연결을 남기면 나중에 질문 흐름을 이해할 수 있다.

질문과 답변이 서로 참조하는 것처럼 보여도 시간 순서로는 Q1 생성 → A1 저장 → Q2 생성이다. 기존 답변을 참조해 다음 질문을 만드는 흐름이다. 이를 이유로 모든 Entity에 무조건 양방향 관계를 붙일 필요는 없다.

SUGGESTED는 후보이고 ASKED는 실제 출제다. 만들어 놓기만 한 후보를 실제 답한 질문 수에 넣으면 기록이 부풀려진다. SKIPPED의 구체적인 동작과 질문 수 제한은 앞으로 정할 부분이다.

최대 10개, 꼬리질문 1~2개는 확정 숫자가 아니다. 제한을 서버에서 관리하고 isComplete로 프론트에 알려준다는 방향이 핵심이다.

## 11. AI 출력을 구조화하고 DB에 매핑하는 이유

AI가 긴 감상문만 반환하면 서버가 “어디가 점수이고 어디가 개선 팁인가”를 안정적으로 구분하기 어렵다. 필드가 정해진 JSON은 저장과 화면 표시의 약속이 된다.

```json
{
  "overallScore": 82,
  "evaluationSummary": "질문에 적절하게 답변했습니다.",
  "strengths": ["본인의 역할을 설명했습니다."],
  "weaknesses": ["결과 설명이 부족합니다."],
  "coachingSummary": "성과와 배운 점을 보완하세요.",
  "improvementTips": ["실제 결과를 구체적으로 설명하세요."],
  "improvedAnswer": "大学のチームプロジェクトでは…",
  "scores": [
    {
      "criterion": "SPECIFICITY",
      "score": 78,
      "feedback": "결과가 더 구체적이면 좋겠습니다.",
      "applicable": true
    }
  ]
}
```

위는 구조 설명용 예시다. scores 바깥의 평가·코칭은 interview_answer_feedbacks, scores의 각 원소는 interview_evaluation_scores로 나누어 저장한다. camelCase API 필드와 snake_case DB 컬럼의 이름이 달라도 의미를 대응시키면 된다.

서버는 구조화된 출력도 검증해야 한다. 잘못된 항목명, 숫자가 아닌 점수, 해당 세션에 없는 messageId를 그대로 저장해서는 안 된다. 구체적인 스키마·실패 처리 방식은 구현 단계에서 정한다.

general conversation의 corrections도 같은 원리다. 전체 summary와 nextTip은 conversation_feedbacks에, 교정 한 건씩은 conversation_corrections에 저장한다. AI에게 일반회화용 평가와 면접용 평가를 동일한 출력으로 강제할 필요는 없다.

또한 id, 사용자 소유권, 생성 시각, 저장 상태는 서버 책임이다. AI가 임의로 정한 사용자의 데이터에 쓰도록 맡기는 구조가 아니다.

## 12. Reflection 최종 결과를 저장하는 이유

면접 평가는 Question Analyzer → Answer Evaluator → Reflection → Interview Coach 순서로 진행한다.

Evaluator가 최초 판단을 만들고 Reflection이 일관성과 중복 등을 검토한다. 화면용 평가를 최초 결과와 검토 결과 두 벌로 저장하면 어느 것이 최종인지 애매해진다. 따라서 Reflection 검토를 마친 평가를 정식 데이터로 저장한다. Coach의 코칭 요약·개선 팁·개선 답변도 함께 저장한다.

이것이 검토 단계가 항상 옳다는 뜻은 아니다. 현재 서비스가 어떤 결과를 최종 평가로 취급할지 기준을 정한 것이다. 별도 디버깅 로그가 필요하면 추후 고려할 수 있지만 현재 14개 테이블에 AI 실행 로그 테이블을 추가한 것은 아니다.

기존 Coach가 만들던 모국어/목표어 답변 두 개도 그대로 가져오지 않았다. 이번 서비스에서는 일본어 개선 답변 improved_answer 하나로 줄이기로 했다.

## 13. evaluation_scores를 행으로 저장하는 이유

고정 컬럼 방식은 logical_score, specificity_score, communication_score처럼 항목마다 컬럼을 만든다. 현재 구조는 criterion과 score를 두고 항목마다 한 행을 만든다.

| answer_feedback_id | criterion | score | applicable |
|---|---|---|---|
| 701 | COMMUNICATION | 85 | true |
| 701 | SPECIFICITY | 78 | true |
| 701 | STAR_STRUCTURE | null | false |

평가 항목이 달라져도 행을 추가하는 방식으로 대응하기 쉽고, 특정 항목의 점수를 조회하기도 좋다. (answer_feedback_id, criterion) UNIQUE는 같은 답변 평가에 같은 항목이 두 번 저장되는 것을 막는다.

### applicable=false는 낮은 점수가 아니다

“자기소개를 해주세요”처럼 모든 답변에 STAR를 강제하기 어려운 질문이 있을 수 있다. 평가 대상이 아니면 score=null, applicable=false로 표현한다.

예를 들어 두 적용 항목 점수가 80, 90이고 STAR가 적용 불가라면, 이를 0점으로 바꾸어 (80+90+0)/3으로 계산하면 학습자가 부당하게 낮은 점수를 받는다. 이 계산은 차이를 이해하기 위한 예시이며 최종 서비스 산식이 아니다.

rubric_version은 기준의 버전이다. 기존 Agent의 5개 항목에 QUESTION_RELEVANCE와 BUSINESS_JAPANESE를 추가하면 같은 이름의 overallScore라도 의미가 달라질 수 있다. 버전을 남겨야 해석할 수 있다.

질문 적합성은 질문에 답했는지, 직무 적합성은 직무와 관련 있는 역량을 보여줬는지다. 둘은 같지 않다. 현재 방향은 이 항목들을 수용하지만 정확한 점수 범위·가중치·종합 계산식은 아직 확정되지 않았다.

강점·약점·개선 팁은 순서대로 보여줄 짧은 목록이어서 JSON으로 두었다. 항목별 점수는 criterion 기준으로 조회하고 다루기 때문에 별도 행으로 분리했다.

## 14. 답변별 평가와 전체 면접 평가를 분리하는 이유

A1에 대한 코칭은 “성과 설명을 보완하세요”이고 A2에 대한 코칭은 “결론부터 말하세요”일 수 있다. 전체 면접 피드백은 여러 답변을 살펴 반복된 강점과 개선점을 설명해야 한다.

```text
Q1 → A1 → 답변별 평가 1
Q2 → A2 → 답변별 평가 2
Q3 → A3 → 답변별 평가 3
                   ↓
전체 질문·답변·최종 평가를 종합
                   ↓
interview_feedbacks 1행
```

interview_answer_feedbacks의 overall_score는 “이 답변의 종합 점수”이고 interview_feedbacks의 overall_score는 “이 면접 전체의 종합 점수”다. 컬럼 이름이 같아도 평가 단위가 다르다.

기존 Coach의 코칭 요약을 그대로 세션 전체 총평으로 쓰면 마지막 답변에 대한 조언을 전체 평가처럼 보여줄 수 있다. 별도 종합 단계가 필요한 이유다.

API는 이 구분을 유지하면서 overall과 answers를 함께 반환한다. 저장 구조는 여러 테이블이지만 화면은 한 요청으로 읽을 수 있다.

## 15. 일반회화 피드백을 가볍게 둔 이유

일반회화에서는 대화를 자연스럽게 이어가고 표현을 개선하는 것이 중심이다. 면접처럼 모든 발화를 rubric으로 채점하는 구조를 채택하지 않았다.

conversation_feedbacks는 전체 요약, 자연스러움·문법·어휘 설명, 강점, 다음 팁을 담는다. conversation_corrections는 고칠 표현이 있을 때 여러 행으로 저장한다.

예를 들어 친구와 카페에서 “私はコーヒーを飲みたいです。”라고 말한 경우 더 편한 표현 “コーヒー飲みたいな。”를 제안할 수 있다. 이는 반드시 문법 오류라는 뜻이 아니므로 NATURALNESS와 GRAMMAR 같은 category를 구분한다.

교정이 없으면 corrections가 빈 배열일 수 있다. 모든 발화에 억지로 교정 한 건을 만드는 구조가 아니다. message_id가 NULL을 허용하므로 모든 교정에 원문 연결이 반드시 있는 것도 아니다.

## 16. Manager/Admin 권한과 학습 데이터의 경계

Manager는 같은 기관의 학습자 가입 상태와 이용현황을 관리한다. 학습자가 어떤 개인 경험을 답했는지, AI에게 어떤 약점을 지적받았는지 열람하는 역할로 합의하지 않았다.

따라서 “자기 기관 사용자니까 모든 정보에 접근 가능하다”는 해석은 틀리다. 같은 기관이라는 조건은 관리 대상 범위를 정할 뿐, 상세 학습 기록 권한까지 부여하지 않는다.

Admin도 기관·Manager·전체 이용 집계를 관리한다. 관리자라는 이름만으로 합의에 없는 학습자 원문 조회 API를 추가하지 않는다.

목록에 studyCount나 lastActivityAt을 제공하는 것과 특정 답변·피드백 원문을 제공하는 것은 다르다. 화면의 버튼을 숨기는 데 그치지 않고 API의 반환 데이터와 접근 검사에 이 경계를 반영해야 한다.

## 17. CRUD와 soft delete(INACTIVE)

CRUD는 Create(생성), Read(조회), Update(수정), Delete(삭제)다. 보통 POST, GET, PATCH, DELETE와 연결해 생각할 수 있지만 기능마다 네 가지를 모두 구현해야 하는 것은 아니다.

우리 기관 관리는 등록·조회·수정과 상태 변경을 제공한다. 기관 행을 실제로 삭제하면 연결된 사용자와 학습 기록의 소속 관계를 어떻게 처리할지 문제가 생긴다. 현재는 INACTIVE로 바꾸어 운영을 중단하고 기존 연결은 남긴다.

```text
기관 등록: POST → ACTIVE
기관 중단: PATCH /deactivate → INACTIVE
기관 재개: PATCH /activate → ACTIVE
```

이처럼 삭제 대신 비활성 상태를 두는 방식을 넓은 의미의 soft delete로 이해할 수 있다. 현재 스키마는 deleted_at을 새로 쓰는 방식이 아니라 status=INACTIVE 방식이다.

기관 비활성 시 가입 선택 목록에서 제외되는 것은 확정이다. 소속 사용자 전체를 자동으로 INACTIVE로 바꾸거나 진행 중 세션을 종료한다는 정책까지 확정된 것은 아니다. 별도 합의 없이 연쇄 동작을 추가하지 않는다.

## 18. 통계 테이블 없이 sessions를 집계하는 MVP 판단

MVP에서는 이미 저장한 세션 수·모드·시간으로 Dashboard를 계산할 수 있다.

- 학습 횟수: 대상 세션 수
- 면접 횟수: session_type=INTERVIEW인 대상 세션 수
- 학습 시간: 대상 duration_seconds 합계
- 기관 범위: users.organization_id로 사용자·세션 범위 제한
- 최근 활동: 대상 사용자의 세션 시각 등을 바탕으로 계산

통계 전용 테이블을 먼저 만들면 세션 생성·종료 때 통계값도 함께 갱신해야 하고, 실패 시 원본과 집계값이 어긋날 수 있다. 현재 규모에서는 원본 sessions에서 읽어 계산하는 쪽을 선택했다.

이것이 모든 규모에서 집계 테이블이 불필요하다는 뜻은 아니다. 데이터가 늘어 실제 조회 비용이 문제가 될 때 별도 집계 방식을 검토하면 된다. 현재 14개 테이블에는 추가하지 않는다.

또한 평균 학습시간의 분모가 사용자 수인지 세션 수인지, ABORTED 시간을 포함할지, 월간 기준 시간대가 무엇인지는 따로 정해야 한다. “집계한다”는 방향과 “정확한 통계 공식”은 구분한다.

## 19. 프론트-ERD 불일치를 어떻게 읽어야 할까?

화면에 입력란이 있다고 DB 컬럼이 이미 있다는 뜻은 아니다. 이번 대화에서 발견된 주요 사례는 다음과 같다.

| 화면/기존 구현 | 최신 합의 | 수정 방향 |
|---|---|---|
| 기관 등록의 담당자 이메일 | organizations에는 해당 컬럼 없음 | Manager 별도 가입·승인 흐름 사용 |
| 기관 등록의 계약 시작일 | 해당 컬럼 없음 | MVP 입력에서 정리. 계약 기능은 후속 논의 |
| 기관 등록의 서비스 상태 | status만 존재 | 생성은 ACTIVE, 이후 activate/deactivate |
| Manager 기관명 직접 입력 | 기존 기관 선택 | signup-options Select Box |
| 기본 난이도·자막 저장 위치 없음 | user_preferences 추가 | preferences API 연결 |
| 프론트 question index로 면접 끝 판단 | 서버 isComplete | 답변 응답 기준으로 종료 흐름 연결 |
| 일반회화 점수 표시 가능성 | 일반회화 무점수 | summary·corrections 중심으로 맞춤 |
| 기존 Agent 5개 항목 | 질문 적합성·비즈니스 일본어 수용 방향 | 새 rubric·AI 출력·화면을 함께 대조 |

DB에 없는 UI 항목을 임의로 다른 컬럼에 넣는 것은 해결이 아니다. 현재 MVP 합의에 맞게 화면을 줄이거나, 기능이 정말 필요할 때 스키마 변경을 다시 논의해야 한다.

초기 분석에 등장한 translated_text, model_answer_native, model_answer_target, messages의 interview_answer_id는 최신 ERD의 컬럼이 아니다. 개선 답변은 improved_answer를 사용한다. 오래된 설명과 최신 표를 섞지 않는 것이 중요하다.

## 20. 처음부터 끝까지 따라 읽는 예시

### 일반회화 한 번

1. Settings에서 중급·일본어/한국어 자막 기본값을 읽는다.
2. Setup에서 카페·친구·밝은 성격을 선택한다.
3. POST /api/conversations로 세션과 당시 설정을 만든다.
4. STT 텍스트를 turns에 보내고 USER/AI 메시지를 저장한다.
5. complete에서 종료 시각과 시간을 기록하고 전체 피드백·교정을 만든다.
6. feedback에서 결과를 읽는다.
7. 나중에 History는 같은 원본을 조합해서 다시 보여준다.

### 면접 한 번

1. Setup에서 직무·난이도·자막을 선택한다.
2. POST /api/interviews로 세션·settings·첫 질문을 만든다.
3. 답변 완료 시 해당 questionId의 answerText를 확정한다.
4. 질문 분석·평가·Reflection·Coach 결과를 저장한다.
5. 꼬리질문이 필요하면 부모 질문과 근거 답변을 연결한다.
6. nextQuestion을 표시하고 반복한다.
7. isComplete=true가 오면 complete로 정상 종료·전체 평가를 진행한다.
8. feedback에서 전체 요약과 답변별 상세를 함께 읽는다.

중간에 중단하면 abort로 ABORTED를 기록하고 Setup으로 돌아간다. 정상 종료와 같은 종합평가 흐름으로 보내지 않는다.

## 21. 스스로 확인할 질문

1. 기본 난이도를 바꿨는데 지난 면접의 난이도가 바뀌면 어느 분리가 잘못된 것일까?
2. JWT로 로그인 사용자를 아는데 body의 userId를 그대로 믿어도 될까?
3. Coach가 만든 한 답변의 요약을 면접 전체 요약으로 써도 될까?
4. STAR 적용 불가를 0점으로 저장하면 어떤 오해가 생길까?
5. Manager가 같은 기관 Learner를 승인할 수 있다는 것이 원문 열람 권한도 뜻할까?
6. 세션 COMPLETED인데 피드백 PROCESSING인 상태가 가능한 이유는 무엇일까?
7. History 화면이 있다고 별도 History 테이블이 꼭 필요할까?

정답의 핵심은 각각 과거 설정 보존, 인증 주체 신뢰, 평가 단위 구분, 미적용과 낮은 성취 구분, 권한 범위 구분, 학습 종료와 생성 완료 구분, 원본 데이터 조합이다.

실제 구현을 시작할 때는 기준 문서의 미정 사항도 함께 확인한다. 특히 질문 제한 숫자, 정확한 점수 공식, 생성 실패·재시도 계약, 페이지네이션과 통계 분모는 이 노트가 새로 확정하지 않는다.

