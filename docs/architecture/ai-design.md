# AI Japanese Speaking Service - AI Design

## 1. 문서 목적

이 문서는 AI Japanese Speaking Service의 AI 기능 구조와 책임을 정의한다.

본 문서는 다음 목적을 가진다.

- Coding Agent가 AI 기능 구현 전 전체 AI 흐름을 이해할 수 있도록 한다.
- Conversation과 Interview AI의 책임을 분리한다.
- STT, LLM, TTS의 역할과 경계를 명확히 한다.
- 불필요한 AI 호출을 방지한다.
- Prompt, Agent, Model 변경이 비즈니스 로직 전체에 영향을 주지 않도록 한다.

ERD, OpenAPI, architecture.md, backend-rules.md의 기존 계약을 우선한다.
본 문서는 새로운 API/DB 계약을 임의로 정의하지 않는다.

---

# 2. AI 전체 구조

서비스의 기본 음성 대화 흐름은 다음과 같다.

사용자 음성
→ STT
→ 사용자 Text
→ Conversation / Interview Service
→ Domain AI Service
→ LLM
→ AI Text
→ TTS
→ AI 음성

학습 종료 후에는 별도의 Feedback 흐름을 수행한다.

Conversation:

전체 Transcript
→ Conversation Feedback AI
→ Structured Feedback
→ DB 저장
→ Feedback 화면

Interview:

질문 + 답변 + Interview Context
→ Interview Evaluation AI
→ Structured Evaluation
→ DB 저장
→ Follow-up Question 생성

---

# 3. AI Layer Architecture

Spring Boot 내부에서 AI 기능은 다음 경계를 유지한다.

Business Service
→ Domain AI Service
→ Common AI Client
→ Spring AI
→ External LLM

예:

ConversationService
→ ConversationAiService
→ AiClient
→ Spring AI
→ OpenAI

ConversationFeedbackService
→ ConversationFeedbackAiService
→ AiClient
→ Spring AI
→ OpenAI

InterviewService
→ InterviewAiService
→ AiClient
→ Spring AI
→ OpenAI

AI Provider와 Model 변경이 Conversation/Interview의 핵심 비즈니스 로직에 직접 영향을 주지 않도록 한다.

별도의 Python AI Server는 사용하지 않는다.

---

# 4. MVP AI Provider

현재 MVP에서는 OpenAI를 사용한다.

목적은 여러 Provider를 비교하는 것이 아니라 짧은 개발 기간 내 실제 음성 학습 E2E를 검증하는 것이다.

구성:

- LLM: OpenAI의 비용 효율적인 모델
- STT: OpenAI Speech-to-Text
- TTS: OpenAI Text-to-Speech
- Conversation Feedback: OpenAI LLM
- Interview Evaluation: 추후 구현

실제 모델명은 구현 시 현재 사용 가능한 Spring AI / OpenAI API와 호환되는 모델을 선택한다.

모델명은 비즈니스 코드 여러 곳에 하드코딩하지 않는다.

OPENAI_API_KEY는 환경변수에서만 읽는다.

실제 API Key를 다음 위치에 저장하지 않는다.

- Java source
- application.yml
- Git repository
- Test source
- Log

---

# 5. Conversation AI

## 5.1 목적

Conversation AI는 사용자의 일본어 회화 상대 역할을 수행한다.

Conversation AI의 목적은 사용자의 답변을 매번 평가하는 것이 아니라 자연스럽게 일본어 대화를 이어가는 것이다.

## 5.2 입력 Context

Conversation AI는 필요에 따라 다음 정보를 사용한다.

- situation
- partnerRole
- partnerPersonality
- situationDescription
- difficulty
- 최근 Conversation Context
- 현재 사용자 발화

## 5.3 출력

기본 출력:

- AI Japanese Response

SubtitleMode가 JAPANESE_KOREAN인 경우 한국어 자막이 필요할 수 있다.

추가 LLM 호출을 만들기보다는 가능한 경우 하나의 응답 생성 과정에서 필요한 데이터를 함께 생성하는 방식을 우선 고려한다.

## 5.4 Difficulty

Difficulty:

- BEGINNER
- INTERMEDIATE
- ADVANCED

MVP에서는 사용자가 직접 선택한다.

Adaptive Difficulty는 MVP 범위에 포함하지 않는다.

난이도에 따라 다음 요소를 조정할 수 있다.

- 어휘 난이도
- 문장 길이
- 표현 복잡도
- AI 응답 길이
- 질문 난이도

---

# 6. Conversation Context Policy

LLM에 전체 Conversation History를 무제한으로 전달하지 않는다.

MVP에서는 자연스러운 대화 유지에 필요한 최소 Context를 사용한다.

초기 MVP에서는 대화 길이가 짧으므로 최근 대화 메시지를 Context로 사용할 수 있다.

향후 필요 시 다음 방식을 고려한다.

- 최근 N Turn
- 이전 Conversation 요약
- Token 기반 Context 제한

이는 향후 최적화 항목이며 현재 MVP에서 복잡한 Context Management를 구현하지 않는다.

---

# 7. Conversation Feedback AI

## 7.1 호출 시점

Conversation Feedback AI는 매 Turn 호출하지 않는다.

학습자가 Conversation을 정상 완료했을 때 전체 Transcript를 기반으로 최대 1회 호출한다.

GET Feedback 요청에서는 AI를 다시 호출하지 않는다.

이미 생성된 Feedback을 DB에서 조회한다.

## 7.2 평가 목적

일반 회화 Feedback은 사용자의 답변 내용 자체의 논리성이나 직무 적합성을 평가하지 않는다.

일본어 표현 품질에 집중한다.

주요 평가 영역:

- 자연스러움
- 문법
- 어휘
- 상황에 적절한 표현
- 더 자연스러운 대체 표현
- 잘한 점
- 다음 학습 Tip

## 7.3 Numeric Score

Conversation Feedback에는 숫자 점수를 사용하지 않는다.

다음과 같은 표현을 생성하지 않는다.

- 자연스러움 80점
- 문법 90점
- 어휘 75점

일반 회화 Feedback은 Text 중심으로 제공한다.

## 7.4 Structured Output

Conversation Feedback은 가능한 경우 Structured Output을 사용한다.

ERD/OpenAPI 계약에 맞는 데이터만 생성한다.

주요 결과:

- summary
- naturalnessComment
- grammarComment
- vocabularyComment
- strengths
- nextTip
- corrections

Correction:

- category
- originalExpression
- suggestedExpression
- explanation
- 관련 message가 식별 가능한 경우 message 연결

AI 응답을 그대로 DB에 저장하기 전에 Parsing/Validation을 수행한다.

---

# 8. Interview AI

Interview 기능은 Conversation과 별도의 AI 책임을 가진다.

MVP의 첫 번째 E2E 테스트 이후 구현한다.

Interview AI는 다음 역할을 담당한다.

- Initial Question Generation
- Question Analysis
- Answer Evaluation
- Evaluation Review
- Coaching
- Follow-up Question Generation
- Final Interview Feedback

기존 InterviewBridge AI의 설계 개념을 재사용할 수 있지만 별도의 Python Agent Server를 유지하지 않는다.

Spring AI 기반으로 재구성한다.

---

# 9. Interview Evaluation

Interview에서는 일본어 표현뿐 아니라 답변 내용도 평가한다.

후보 평가 영역:

- QUESTION_RELEVANCE
- LOGICAL_THINKING
- SPECIFICITY
- DELIVERY
- JOB_FIT
- ANSWER_STRUCTURE
- BUSINESS_JAPANESE

정확한 Criterion 문자열, 가중치, Overall Score 계산 방식은 Interview 구현 전에 확정한다.

따라서 현재 단계에서 Coding Agent가 임의로 Score Formula를 정의하면 안 된다.

평가 대상이 아닌 Criterion은 다음 방식으로 표현한다.

- applicable = false
- score = null

0점으로 처리하지 않는다.

rubric_version을 저장하여 향후 평가 기준 변경을 추적할 수 있도록 한다.

---

# 10. Interview Follow-up

Interview의 Follow-up Question은 사용자의 직전 답변과 Interview Context를 기반으로 생성한다.

Backend/AI가 다음 상태를 결정한다.

- 다음 Follow-up Question 생성
- 다음 Initial Question 진행
- Interview 종료

Frontend가 질문 개수를 기준으로 임의 종료 판단하지 않는다.

API의 다음 구조를 기준으로 한다.

- nextQuestion
- isComplete

Follow-up 횟수와 종료 정책은 Interview 구현 전에 별도로 확정한다.

---

# 11. STT

STT의 역할:

사용자 음성
→ 일본어 Text

MVP에서는 실시간 Streaming STT를 사용하지 않는다.

기본 흐름:

브라우저 MediaRecorder
→ Audio 생성
→ Backend 전송
→ STT API
→ Japanese Text

STT 결과는 Conversation 또는 Interview의 사용자 메시지/답변으로 사용한다.

향후 개선:

- Streaming STT
- Partial Transcript
- 발화 중 실시간 자막

---

# 12. TTS

TTS의 역할:

AI Japanese Text
→ AI Japanese Speech

MVP에서는 실시간 Streaming TTS를 사용하지 않는다.

기본 흐름:

LLM Response
→ TTS API
→ Audio
→ Frontend
→ Browser Audio Playback

향후 음성 품질 요구사항에 따라 TTS Provider를 교체할 수 있도록 LLM 비즈니스 로직과 분리한다.

---

# 13. External AI API Usage Policy

현재 개발에 사용하는 OpenAI API Key는 공용 Key이므로 API 호출량을 엄격하게 제한한다.

## 금지

- Application startup 시 AI 호출
- 페이지 load 시 AI 호출
- Unit Test에서 실제 AI 호출
- Integration Test에서 실제 AI 호출
- Background AI 호출
- 자동 반복 호출
- 자동 retry
- 같은 Feedback 재생성
- 개발 확인을 위한 반복적인 실제 API 호출

## Conversation 호출 원칙

사용자 발화 1회:

STT 최대 1회
→ Conversation LLM 최대 1회
→ TTS 최대 1회

Conversation 완료:

Feedback LLM 최대 1회

Feedback 조회:

DB 조회만 수행
→ LLM 호출 0회

외부 API 실패 시 자동으로 반복 요청하지 않는다.

---

# 14. AI Failure Handling

AI 관련 DB Entity의 generation_status 계약을 유지한다.

상태:

- PENDING
- PROCESSING
- COMPLETED
- FAILED

AI 생성 실패를 일반 DB Transaction 실패와 동일하게 취급하지 않는다.

MVP에서는 복잡한 Queue/Retry System을 구현하지 않는다.

자동 Retry 정책도 도입하지 않는다.

향후 필요 시 다음을 고려한다.

- Background Job
- Queue
- Retry Policy
- Failure Recovery
- Partial Failure Handling

---

# 15. Security

AI API Key는 Backend에서만 사용한다.

Frontend에 OpenAI API Key를 전달하지 않는다.

구조:

React
→ Spring Boot
→ OpenAI

다음 구조를 사용하지 않는다.

React
→ OpenAI API 직접 호출

따라서 브라우저 Network/JavaScript Bundle에 API Key가 노출되어서는 안 된다.

---

# 16. MVP 범위

현재 우선 구현 대상:

1. Conversation Session
2. Speech Recording
3. STT
4. Conversation LLM
5. TTS
6. Conversation Feedback
7. Conversation Feedback UI

현재 우선 구현하지 않는 AI 기능:

- Interview AI
- Adaptive Difficulty
- Streaming STT
- Streaming TTS
- Realtime API
- Long-term Conversation Memory
- Personalized Learning Model
- Multi-Agent Framework
- Vector DB / RAG

---

# 17. Future Improvements

MVP 이후 고려할 수 있다.

## Conversation
- Adaptive Difficulty
- 사용자 반복 오류 분석
- 개인별 약점 추적
- 과거 학습 기반 Personalized Feedback
- Conversation Context 요약

## Interview
- Agent 역할 세분화
- Question Analyzer 개선
- Reflection / Evaluation Review
- Follow-up 정책 개선
- Business Japanese 평가 개선
- Rubric Version 관리

## Speech
- Streaming STT
- Streaming TTS
- 실시간 Conversation
- WebSocket / Realtime API
- Voice Provider 비교

## Infrastructure
- Async AI Processing
- Queue
- Retry / Recovery
- AI Usage Monitoring
- Token/Cost Monitoring

---

# 18. Coding Agent Rules for AI Implementation

AI 기능을 구현하는 Coding Agent는 다음 원칙을 따른다.

1. ERD/OpenAPI 계약을 임의로 변경하지 않는다.
2. AI 기능 때문에 기존 비즈니스 API를 임의로 변경하지 않는다.
3. Prompt를 Controller에 작성하지 않는다.
4. OpenAI Client 호출을 Controller에서 직접 수행하지 않는다.
5. AI Provider 관련 코드를 Conversation/Interview Business Logic에 무분별하게 분산하지 않는다.
6. 실제 API Key를 코드에 기록하지 않는다.
7. Test에서 실제 OpenAI API를 호출하지 않는다.
8. Structured Output은 Parsing/Validation 후 사용한다.
9. Conversation Feedback을 매 Turn 생성하지 않는다.
10. Interview 정책이 미확정된 부분을 임의로 확정하지 않는다.
11. 불필요한 Agent/Framework/Infrastructure를 추가하지 않는다.
12. 구현 범위를 넘어선 AI 기능을 미리 만들지 않는다.