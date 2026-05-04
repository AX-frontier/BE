# AX Prontier API

메타 에이전트 기반 대학 정보 통합 AI 서비스의 Spring Boot 백엔드입니다.

현재 단계에서는 로그인 없이 익명 대화 단위로 사용자의 질문과 AI 응답 이력을 저장하는 프로토타입 구조를 잡았습니다. 백엔드는 대화, 질문, AI 호출 기록, 라우팅 결과, 최종 응답, 문서 검토 결과, 도서 검색 로그, 감사 로그를 관리합니다.

크롤링 원문, 청크, 임베딩, 벡터 검색, 프롬프트, 에이전트 실행 내부 로직, 도서 원천 데이터, 문서 검토 규칙은 AI 서버 또는 RAG 저장소가 소유합니다.

## 기술 스택

```text
Java 21
Spring Boot
Gradle Groovy
PostgreSQL
Spring Data JPA
Spring Web
Validation
```

## 프로젝트 구조

```text
com.axprontier.api
├── global        공통 설정
├── conversation  익명 대화
├── query         질문/응답 흐름
├── ai            AI 서버 연동
├── review        문서 검토
├── library       도서관/도서 검색
└── audit         감사 로그
```

각 기능 패키지는 아래 구조를 기본으로 사용합니다.

```text
controller  API 요청을 받는 곳
service     비즈니스 흐름을 처리하는 곳
repository  DB에 접근하는 곳
entity      DB 테이블과 매핑되는 클래스
dto         요청/응답 데이터 클래스
```

## 기능별 책임

```text
global        공통 설정, 예외, 응답 포맷
conversation  익명 대화 세션
query         사용자 질문, 라우팅 결과, 에이전트 실행 기록, 최종 응답
ai            AI 서버 호출 DTO, 클라이언트, 요청/응답 로그
review        전자결재 문서 검토 요청, 결과, 지적사항
library       도서 검색 요청/결과 로그
audit         감사 로그
```

## 주요 엔티티

```text
Conversation
Query
QueryRoute
AgentRun
QueryResponse
AiRequestLog
AiResponseLog
ReviewRequest
ReviewResult
ReviewFinding
BookSearchLog
AuditLog
```

## 로컬 개발 환경

팀원은 아래 도구를 설치합니다.

```text
Java 21
Docker
Git
```

환경변수 파일을 만듭니다.

```bash
cp .env.example .env
```

로컬 PostgreSQL을 실행합니다.

```bash
docker compose up -d
```

Spring Boot를 실행합니다.

```bash
export $(cat .env | xargs)
./gradlew bootRun
```

## 초기 API

대화를 생성합니다.

```http
POST /api/conversations
```

대화에 질문을 추가하고 AI 오케스트레이터를 호출합니다.

```http
POST /api/conversations/{conversationUid}/queries
```

## Swagger

서버 실행 후 아래 주소에서 API 문서를 확인합니다.

```text
http://localhost:8080/swagger-ui.html
```

## 환경변수

실제 `.env` 파일은 Git에 올리지 않습니다. 레포에는 `.env.example`만 공유합니다.

```text
DB_URL
DB_USERNAME
DB_PASSWORD
AI_SERVER_BASE_URL
CORS_ALLOWED_ORIGINS
```

## 초기 배포 방향

프로토타입 단계에서는 비용과 복잡도를 줄이기 위해 아래 구조로 시작합니다.

```text
FE
↓
Spring Boot BE on EC2
↓
AI Server

Spring Boot BE
↓
RDS PostgreSQL
```

초기에는 EC2 1대와 RDS PostgreSQL Single-AZ 최소 사양으로 시작합니다. NAT Gateway, Load Balancer, Multi-AZ 구성은 사용하지 않고, 필요해지는 시점에 확장합니다.

AWS에서는 배포 전에 Budgets 월 예산 알림을 먼저 설정합니다.
