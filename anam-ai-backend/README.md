# Anam AI Backend

Spring Boot 기반 백엔드 서버 프로젝트

## 개발 환경 설정

### 1. 사전 요구사항
- Java 21
- MySQL 8.0 이상
- Gradle 8.14.3

### 2. 데이터베이스 설정

MySQL에서 데이터베이스를 생성합니다:

```sql
CREATE DATABASE anamwallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 애플리케이션 설정

1. `src/main/resources/application.yml.example` 파일을 복사하여 `application.yml` 생성:
   ```bash
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   ```

2. `application.yml` 파일에서 데이터베이스 정보 수정:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/anamwallet
       username: your_username
       password: your_password
   ```

### 4. 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 실행
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 프로젝트 구조

```
src/main/java/anam_145/SpringBoot/Server/
├── apiPayload/     # API 응답 포맷 관련
├── config/         # 설정 클래스
├── converter/      # 데이터 변환 클래스
├── domain/         # 엔티티 클래스
├── filter/         # 필터 클래스
├── repository/     # JPA 리포지토리
├── scheduler/      # 스케줄러
├── service/        # 비즈니스 로직
├── util/           # 유틸리티 클래스
└── web/            # 컨트롤러
```

## 주의사항

- `application.yml` 파일은 Git에 커밋되지 않습니다 (개인정보 보호)
- 팀원과 공유할 때는 `application.yml.example` 파일을 참고하도록 안내해주세요
