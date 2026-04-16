# 비동기 이미지 처리 시스템 과제 

## 1. 현재 구현 범위
- **이미지 처리 요청 API:** 클라이언트로부터 이미지 가공 요청을 수신하고 `taskId`를 즉시 반환합니다.
- **멱등성 보장 (Idempotency):** 동일한 요청의 중복 처리를 방지하기 위한 제어 로직이 반영되었습니다.
- **메시지 큐잉:** 수신된 요청을 Kafka(`image-process-topic`)로 안전하게 발행합니다.
- **도메인 상태 모델:** 작업의 생명주기를 관리하기 위한 기초 엔티티 설계를 완료했습니다.

## 2. 주요 설계 및 기술적 판단

### 멱등성 보장 및 중복 요청 차단
- 클라이언트가 헤더에 담아 보내는 `Idempotency-Key`를 활용합니다. DB의 유니크 제약 조건(`uk_idempotency_key`)을 통해 동일 키에 대한 동시 요청을 차단하며, 중복 요청 시에는 예외를 캐치하여 기존에 생성된 `taskId`를 반환함으로써 시스템의 안정성을 확보했습니다.

### 계층 분리 및 예외 처리
- **Controller:** 웹 요청을 수신하고 응답 DTO(`TaskResponse`)로 변환하여 반환하는 역할에 집중합니다.
- **Service:** 비즈니스 로직과 외부 메시지 브로커(Kafka) 연동을 담당하며, 도메인 엔티티를 캡슐화하여 보호합니다.
- **Global Exception Handling:** 시스템 전반에서 발생하는 예외(`BusinessException` 등)를 일관된 에러 응답 포맷으로 처리하기 위한 공통 구조를 구축했습니다.

## 3. 테스트 전략
Mock에 과도하게 의존하기보다, 실제 인프라 환경에서의 동작을 보장하는 데 초점을 맞추었습니다.
- **통합 테스트 (Integration Test):** `Testcontainers`를 사용하여 실제 MySQL, Kafka, Redis 컨테이너를 일시적으로 구동한 상태에서 전체 흐름을 검증합니다.
- **E2E 검증:** `TestRestTemplate`을 사용하여 실제 HTTP 네트워크 통신 환경과 동일한 조건에서 API 응답 및 상태 코드를 확인합니다.
- **리포지토리 테스트:** 실제 DB 환경에서 유니크 제약 조건 등 스키마 제약 사항이 정상 작동하여 중복 저장을 방어하는지 검증합니다.

## 4. 로컬 환경 실행 방법

### 애플리케이션 빌드
```bash
./gradlew clean build -x test
```
### 인프라 및 앱 기동 (Docker Compose)
- docker-compose.yml을 통해 MySQL, Redis, Kafka 환경을 한 번에 구축할 수 있습니다.
```bash
docker-compose up -d --build
```
### 테스트 및 커버리지 확인
- JaCoCo 플러그인을 통해 테스트 성공 여부 및 커버리지 리포트를 생성할 수 있습니다.
```bash
./gradlew test jacocoTestReport
```
