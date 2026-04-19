# 비동기 이미지 처리 시스템 과제

## 1. 기능 구현
- **이미지 처리 요청 API:** 클라이언트로부터 가공 요청을 수신하고 `taskId`를 즉시 반환합니다.
- **작업 상태 및 결과 조회 API:** 특정 작업의 진행 상태(`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`)와 결과를 조회할 수 있습니다.
- **외부 API 비동기 워커:** Kafka를 활용해 외부 `Mock Worker` 시스템과 통신하며, 등록 즉시 식별자(`jobId`)를 매핑합니다.
- **지연 동기화:** 클라이언트의 조회 시점에 외부 서버의 상태를 실시간으로 확인하여 최신 상태를 유지합니다.

## 2. 주요 설계 및 기술적 고민

### 2.1 동시 요청과 멱등성 보장
동일한 요청이 중복으로 전달되는 상황을 방어하기 위해 DB 레벨의 유니크 제약 조건(`uk_idempotency_key`)을 사용했습니다. 중복 요청 시 기존 생성된 `taskId`를 반환하여 데이터 일관성을 보장합니다.

### 2.2 트랜잭션 경계 분리 및 Non-blocking Worker
외부 API 호출은 네트워크 지연이 발생할 수 있으므로 DB 트랜잭션 외부에서 수행되도록 설계했습니다. 워커는 외부 서버에 작업을 등록하고 `jobId`를 매핑하는 즉시 Kafka 메시지를 커밋(`ack`)하여 컨슈머 스레드가 블로킹되는 것을 방지합니다.

### 2.3 리소스 효율화를 위한 가상 스레드
I/O 집약적인 Kafka Consumer 작업에 Java 21의 가상 스레드(Virtual Threads)를 적용했습니다. 이를 통해 플랫폼 스레드 점유율을 낮추고 대규모 비동기 요청 처리 능력을 확보했습니다.

## 3. 상태 모델 및 데이터 정합성 보장

### 3.1 상태 전이 흐름
- **Flow:** `PENDING` ➔ `PROCESSING` ➔ `COMPLETED` / `FAILED`
- 엔티티 내부 로직을 통해 유효하지 않은 상태 전이(예: 완료된 작업의 재실행)를 원천 차단했습니다.

### 3.2 서버 재시작 시의 데이터 복구 (Lazy Sync)
- **문제 해결:** 과거 서버 다운 시 작업이 `PROCESSING` 상태로 고착되는 한계를 **지연 동기화** 방식으로 해결했습니다.
- **동작 원리:** 워커가 외부 작업 등록 시 부여받은 `jobId`를 DB에 영속화합니다. 이후 서버가 재시작되더라도 클라이언트의 상태 조회 요청 시 저장된 `jobId`를 통해 외부 서버의 최신 상태를 확인하고 우리 DB를 자동으로 동기화합니다.

## 4. API 명세 및 문서화
본 프로젝트는 **Spring REST Docs**를 통해 신뢰할 수 있는 API 문서를 제공합니다.

| 문서명 | 경로 | 설명 |
| :--- | :--- | :--- |
| **API 가이드** | `/docs/index.html` | 전체 API의 요청/응답 스펙 및 예시를 제공합니다. |

## 5. 테스트 전략
- **통합 테스트:** `Testcontainers`를 사용하여 실제 MySQL, Kafka 환경에서의 유실 없는 메시지 처리를 검증합니다.
- **E2E 검증:** `TestRestTemplate`을 통해 비동기 처리 완료 후 상태 동기화가 정상적으로 이루어지는지 전체 흐름을 확인합니다.

## 6. 로컬 환경 실행 방법

### 애플리케이션 빌드
```bash
./gradlew clean build -x test
```
### 인프라 및 앱 기동 (Docker Compose)
- docker-compose.yml을 통해 MySQL, Kafka 환경을 한 번에 구축할 수 있습니다.
```bash
docker-compose up -d --build
```
### 테스트 및 커버리지 확인
- JaCoCo 플러그인을 통해 테스트 성공 여부 및 커버리지 리포트를 생성할 수 있습니다.
- `Testcontainers` 를 사용한 통합 테스트 실행을 위해 반드시 `Docker`를 실행 후 테스트를 실행해야 합니다.
```bash
./gradlew test jacocoTestReport
```
```bash
open build/reports/jacoco/test/html/index.html
```
### API 명세 확인
- Spring REST Docs를 사용하여 실제 테스트 케이스를 통과한 신뢰성 높은 명세를 제공합니다.
- 애플리케이션 기동 후 아래 주소로 접속하거나, 빌드 후 생성된 로컬 파일(`build/docs/asciidoc/index.html`)을 브라우저로 열어 직접 확인할 수 있습니다.
- 접속 주소: http://localhost:8080/docs/index.html
