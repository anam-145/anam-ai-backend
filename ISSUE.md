# Feat: MiniApp AI 가이드 시스템 구현 (Simple RAG)

## 📋 개요
MiniApp 사용자에게 코드 기반 AI 가이드를 제공하는 시스템을 구현합니다.
Simple RAG 아키텍처를 사용하여 Vector DB 없이 MySQL + GPT-4만으로 구현합니다.

## 🎯 목표
- MiniApp Kotlin 코드를 AST 파싱하여 UI 요소 정보 추출
- MySQL Full-Text Search로 관련 UI 요소 검색
- GPT-4를 활용한 단계별 가이드 생성
- 실시간 화면 하이라이트 + 말풍선 안내 (클라이언트 연동)

## 🏗️ 아키텍처

### Phase 1: MiniApp 승인 시
```
ZIP 업로드 (Kotlin 파일)
    ↓
AST 파서로 UI 요소 분석
  - Button, TextField 등 추출
  - id, text, semanticHint, onClick 등 정보 수집
    ↓
MySQL에 인덱싱 (Full-Text Index)
```

### Phase 2: 사용자 질문 시
```
"송금하는 방법?"
    ↓
키워드 추출 및 MySQL Full-Text Search
    ↓
관련 UI 요소 50개 추출
    ↓
전체 컨텍스트를 GPT-4에게 전달
    ↓
GPT-4가 분석 + 단계별 가이드 생성
    ↓
클라이언트에서 화면 하이라이트
```

## 📦 구현 범위

### 1. 의존성 추가 (`build.gradle`)
- [ ] `kotlin-compiler-embeddable` (AST 파싱)
- [ ] `openai-java` (GPT-4 API)
- [ ] `commons-compress` (ZIP 처리)

### 2. 도메인 모델 (Entity)
- [ ] `MiniAppCodeIndex` - MiniApp 코드 인덱스
- [ ] `ScreenInfo` - 화면 정보
- [ ] `ComposableInfo` - UI 요소 정보 (Button, TextField 등)
- [ ] `GuideEntity` - 생성된 가이드
- [ ] `GuideStepEntity` - 가이드 단계

### 3. Repository
- [ ] `ComposableInfoRepository`
  - Full-Text Search 쿼리 구현
  - `findByFullText(appId, keywords, limit)`
- [ ] `ScreenInfoRepository`
- [ ] `MiniAppCodeIndexRepository`
- [ ] `GuideRepository`

### 4. 서비스 레이어
- [ ] **ZipExtractorService** - ZIP 파일 추출
  - `extractKotlinFiles(MultipartFile zipFile)`
- [ ] **KotlinASTParser** - Kotlin 코드 파싱
  - `parseKotlinFile(String content)`
  - `parseComposable()`
  - `parseButton()`
  - `parseTextField()`
  - `extractNamedArguments()`
- [ ] **CodeIndexService** - 코드 인덱싱 및 검색
  - `indexMiniAppCode(String appId, MultipartFile zipFile)`
  - `searchUIElements(String appId, String query)`
- [ ] **OpenAIService** - OpenAI API 호출
  - `generateGuide(String prompt)`
  - Function Calling 구현
- [ ] **GuideGenerationService** - 가이드 생성
  - `generateGuide(GuideGenerateRequest request)`
  - `buildContext(List<ComposableInfo> uiElements)`
  - `buildPrompt(String query, String context)`

### 5. API 엔드포인트
- [ ] `POST /api/admin/miniapp/approve`
  - Request: `appId`, `zipFile` (MultipartFile)
  - Response: `ApproveResponse` (screensIndexed, composablesIndexed)
- [ ] `POST /api/guide/generate`
  - Request: `GuideGenerateRequest` (query, appId, currentScreen)
  - Response: `GuideResponse` (guideId, intent, steps)
- [ ] `POST /api/guide/step/complete`
  - Request: `StepCompleteRequest` (guideId, stepNumber, result)
  - Response: `NextStepResponse` (hasNext, nextStep)

### 6. DTO 클래스
- [ ] `ApproveResponse`
- [ ] `GuideGenerateRequest`
- [ ] `GuideResponse`
- [ ] `GuideStepDTO`
- [ ] `StepCompleteRequest`
- [ ] `NextStepResponse`

### 7. 예외 처리
- [ ] `MiniAppNotFoundException`
- [ ] `CodeParsingException`
- [ ] `GuideGenerationException`
- [ ] `OpenAIAPIException`

## 🗄️ 데이터베이스 스키마

### `mini_app_code_index`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| app_id | VARCHAR(100) PK | MiniApp ID |
| indexed_at | DATETIME | 인덱싱 시간 |

### `screen_info`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 증가 |
| app_id | VARCHAR(100) | MiniApp ID |
| name | VARCHAR(200) | 화면 이름 (예: TransferScreen) |
| source_file | VARCHAR(500) | 소스 파일 경로 |

### `composable_info` ⭐
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 증가 |
| app_id | VARCHAR(100) | MiniApp ID |
| screen_id | BIGINT FK | 소속 화면 |
| type | VARCHAR(50) | UI 타입 (Button, TextField) |
| composable_id | VARCHAR(200) | UI 식별자 |
| text | VARCHAR(500) | 표시 텍스트 |
| semantic_hint | VARCHAR(500) | 의미적 힌트 (검색용) |
| onclick_code | TEXT | 클릭 동작 코드 |
| modifier_code | TEXT | Modifier 코드 |
| source_file | VARCHAR(500) | 소스 파일 |
| line_number | INT | 라인 번호 |
| searchable_text | TEXT | 전체 검색용 텍스트 |

**FULLTEXT INDEX**: `searchable_text`, `text`, `semantic_hint`, `onclick_code`

### `guide_entity`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| guide_id | VARCHAR(100) PK | 가이드 ID |
| app_id | VARCHAR(100) | MiniApp ID |
| intent | VARCHAR(50) | 의도 (TRANSFER, RECEIVE 등) |
| user_query | VARCHAR(1000) | 사용자 질문 |
| created_at | DATETIME | 생성 시간 |

### `guide_step_entity`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 증가 |
| guide_id | VARCHAR(100) FK | 가이드 ID |
| step_number | INT | 단계 번호 |
| instruction | VARCHAR(1000) | 안내 메시지 |
| target_screen | VARCHAR(200) | 대상 화면 |
| target_element | VARCHAR(200) | 대상 UI 요소 ID |
| highlight_bounds | TEXT | 하이라이트 영역 (JSON) |

## 🔧 기술 스택
- **Backend**: Spring Boot 3.5.6 + Java 21
- **Database**: MySQL 8.0
- **AI**: OpenAI GPT-4 API
- **Parser**: Kotlin Compiler Embeddable

## 📊 예상 비용
- OpenAI GPT-4 API: ~$20/월 (1000 요청 기준)
- MySQL: $0 (기존 사용 중)

## ✅ 완료 조건
- [ ] MiniApp ZIP 업로드 시 Kotlin 코드가 파싱되어 DB에 저장됨
- [ ] Full-Text Search로 관련 UI 요소를 정확히 찾을 수 있음
- [ ] 사용자 질문에 대해 GPT-4가 적절한 단계별 가이드를 생성함
- [ ] API 엔드포인트가 정상 동작함
- [ ] 에러 처리가 적절히 구현됨

## 📚 참고 문서
- 기획서: `25078889-0bbd-44a3-bfae-57eaf87e5de6_1013_-_AnamWallet_AI_가이드_시스템_기획서.pdf`
- Kotlin Compiler API: https://kotlinlang.org/docs/compiler-reference.html
- OpenAI API: https://platform.openai.com/docs/guides/function-calling

## 🚀 구현 순서
1. 의존성 추가 및 환경 설정
2. Entity 및 Repository 구현
3. AST 파서 구현 (간단한 UI만)
4. 코드 인덱싱 서비스 구현
5. OpenAI 연동 및 가이드 생성 서비스 구현
6. API 엔드포인트 구현
7. 테스트 및 검증

## ⚠️ 주의사항
- 초기에는 Button, TextField, Text 등 기본 Composable만 지원
- 복잡한 중첩 구조는 점진적으로 추가
- OpenAI API 키는 환경변수로 관리 (application.yml)
- Full-Text Search는 MySQL 5.7+ 이상 필요

## 💾 GitHub 이슈 생성 방법
1. GitHub 저장소로 이동
2. Issues 탭 클릭
3. New Issue 클릭
4. 이 파일의 내용을 복사하여 붙여넣기
5. Label: `enhancement`, `feature`
6. Milestone: 해당하는 경우 설정
