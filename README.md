# 비동기 이미지 처리 시스템 과제

## 1. 현재 구현 범위
- **이미지 처리 요청 API:** 클라이언트로부터 이미지 가공 요청을 수신하고 `taskId`를 즉시 반환합니다.
- **멱등성 보장 (Idempotency):** 동일한 요청의 중복 처리를 방지하기 위한 제어 로직이 반영되었습니다.
- **메시지 큐잉:** 수신된 요청을 Kafka(`image-process-topic`)로 안전하게 발행합니다.
- **도메인 상태 모델:** 작업의 생명주기를 관리하기 위한 기초 엔티티 설계를 완료했습니다.
- **비동기 외부 API 워커:** Kafka에서 메시지를 소비하여 외부 `Mock Worker` 시스템과 HTTP 통신을 수행합니다.
- **재시도(Retry) 및 장애 격리:** 외부 시스템의 불안정한 응답(Timeout, 5xx 등)에 대비한 지수 백오프 재시도 로직을 구축했습니다.

---

## 2. 주요 설계 및 기술적 판단

### 2.1 멱등성 보장 및 중복 요청 차단
- 클라이언트가 헤더에 담아 보내는 `Idempotency-Key`를 활용합니다.
- DB의 유니크 제약 조건(`uk_idempotency_key`)을 통해 동일 키에 대한 동시 요청(Race Condition)을 원천 차단합니다. 중복 요청 시에는 `DataIntegrityViolationException`을 캐치하여 기존에 생성된 `taskId`를 안전하게 반환함으로써 시스템의 일관성을 확보했습니다.

### 2.2 계층 분리 및 DB 트랜잭션 분리
- **Controller:** 웹 요청 수신 및 응답 DTO(`TaskResponse`) 변환 역할에 집중합니다.
- **Service:** 트랜잭션(`@Transactional`)의 경계를 관리합니다. 특히 외부 API 호출 시 DB 커넥션 풀 고갈을 막기 위해 '상태 변경(PROCESSING)'과 '완료/실패 마킹' 트랜잭션을 분리하여 짧게 유지했습니다.
- **Worker:** 비즈니스 흐름을 제어하는 오케스트레이터 역할을 수행하며, 트랜잭션에 속하지 않은 상태에서 외부 API 통신을 전담합니다.
- **Global Exception Handling:** `BusinessException`을 통해 일관된 에러 응답 포맷을 제공합니다.

### 2.3 외부 시스템(Mock Worker) 연동 방식 및 선택 이유
- **Spring Core RetryTemplate 도입:** 외부 API의 응답 시간과 안정성을 신뢰할 수 없으므로, 최대 3회 재시도 및 지수 백오프(Exponential Backoff) 전략을 적용했습니다. AOP 프록시 제약과 테스트 용이성을 고려해 어노테이션 방식 대신 `RetryTemplate` 빈을 명시적으로 주입하여 제어권을 강화했습니다.
- **가상 스레드 기반 Consumer:** 수십 초가 소요될 수 있는 I/O 블로킹 작업 시, 기존 플랫폼 스레드 풀을 사용할 경우 스레드 고갈 병목(Bottleneck)이 발생합니다. 이를 극복하기 위해 Spring Boot 4.x(Java 21)의 가상 스레드를 Kafka Consumer TaskExecutor에 적용하여 리소스 효율성을 극대화했습니다.

---

## 3. 아키텍처 정의

### 3.1 작업 상태 모델 및 상태 전이
- 작업의 생명주기를 `PENDING` ➔ `PROCESSING` ➔ `COMPLETED` / `FAILED` 4단계로 정의했습니다.
- **허용되지 않는 상태 전이:** 이미 처리가 시작된 `PROCESSING` 상태의 작업이 다시 `PENDING`으로 돌아가거나, `COMPLETED/FAILED` 등 종결된 작업이 다시 `PROCESSING`으로 전이되는 것을 도메인 엔티티 내부 검증 로직을 통해 강력히 차단합니다.

### 3.2 처리 보장 모델 (Processing Guarantee)
본 시스템은 **적어도 한 번(At-Least-Once)** 처리 모델을 채택하고 있습니다.
- **판단 근거:** Kafka Consumer의 `enable-auto-commit`을 `false`로 설정하고, DB 상태 업데이트(`COMPLETED/FAILED`)가 완전히 끝난 직후 `finally` 블록에서 수동으로 `ack.acknowledge()`를 호출하도록 설계했습니다.
- 외부 API 통신 중 서버가 다운되더라도 카프카 오프셋이 커밋되지 않았으므로 재시작 시 메시지를 다시 소비합니다. 이때 `ImageTask` 도메인의 멱등성 검증(이미 처리된 상태인지 확인)을 통해 실제 외부 API의 중복 호출을 안전하게 방어합니다.

### 3.3 서버 재시작 시 동작 및 데이터 정합성 한계
- 서버가 강제 종료(Kill)되어 재시작되는 경우, 처리 전(PENDING)인 메시지는 카프카를 통해 다시 소비되어 정상 처리됩니다.
- **데이터 정합성 깨짐 지점 (Zombie Task):** DB 상태를 `PROCESSING`으로 변경하고 커밋한 직후, 외부 API를 호출하는 도중에 서버가 다운된다면 해당 작업은 `PROCESSING` 상태로 영구히 남게 됩니다. 카프카 메시지는 재소비되지만, 도메인 로직이 `INVALID_TASK_STATUS` 예외를 던지며 처리를 거부하기 때문입니다.
- *(해결 방향)* 이를 완벽히 해결하기 위해서는 일정 시간 이상 `PROCESSING`에 머물러 있는 태스크를 스윕(Sweep)하여 `FAILED`로 변경하거나 다시 큐에 넣는 **Reconciliation Batch(대사 배치)** 가 추가로 필요합니다.

---

## 3. 테스트 전략
Mock에 과도하게 의존하기보다, 실제 인프라 환경에서의 동작을 보장하는 데 초점을 맞추었습니다.
- **통합 테스트 (Integration Test):** `Testcontainers`를 사용하여 실제 MySQL, Kafka, Redis 컨테이너를 일시적으로 구동한 상태에서 전체 흐름을 검증합니다.
- **E2E 검증:** `TestRestTemplate`을 사용하여 실제 HTTP 네트워크 통신 환경과 동일한 조건에서 API 응답 및 상태 코드를 확인합니다.
- **리포지토리 테스트:** 실제 DB 환경에서 유니크 제약 조건 등 스키마 제약 사항이 정상 작동하여 중복 저장을 방어하는지 검증합니다.

---

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
