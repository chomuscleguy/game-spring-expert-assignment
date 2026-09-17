## 필수 기능 구현

**Lv1. Docker로 MySQL과 Redis 설정**
- [x] Docker로 MySQL과 Redis를 실행합니다.
- [x] Spring 애플리케이션의 환경 변수를 설정합니다.

<details>
<summary><b>[자세히] </b></summary>

1. Dockerfile 구성
* 멀티 스테이지 빌드로 구성 — 빌드 스테이지(JDK 21)에서 `.jar`를 생성하고, 실행 스테이지(JRE 21)로 산출물만 복사
* JDK가 아닌 JRE 이미지로 실행하여 최종 이미지 용량 절감
* Windows 개행문자(CRLF) 변환 처리로 `gradlew` 실행 오류 방지

 [Dockerfile 바로가기](./Dockerfile)

---

2. 애플리케이션 아티팩트 빌드
* 별도의 로컬 빌드 없이 **Docker 이미지 빌드 과정에서 `.jar` 생성**
* 의존성 선언 파일을 소스보다 먼저 복사해 레이어 캐시 재사용

```dockerfile
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar -x test --no-daemon
```

---

3. docker-compose.yaml 구성
MySQL · Redis · Spring Boot 애플리케이션 컨테이너 묶음 관리

데이터 영속성을 위한 볼륨 마운트(`mysql_data`, `redis_data`) 및 healthcheck 조건 적용

`depends_on` + `condition: service_healthy` 로 **MySQL과 Redis가 완전히 준비된 뒤에** 애플리케이션 기동

 [docker-compose.yaml 바로가기](./docker-compose.yaml)

| 서비스 | 이미지 | 컨테이너명 | 포트 |
|---|---|---|---|
| `db` | `mysql:8.0` | `expert-assignment-mysql` | `3306` |
| `redis` | `redis:7-alpine` | `expert-assignment-redis` | `6379` |
| `app` | 로컬 `Dockerfile` 빌드 | `expert-assignment-app` | `8080` |

---

4. 환경 변수 세팅 (.env)
DB · Redis 접속 정보 및 루트 비밀번호 등 보안 민감 정보 분리 (`.gitignore` 처리)

협업 및 평가용 환경 변수 스키마 제공

 [.env.example 바로가기](./.env.example)

`.env`의 값을 Compose가 읽어 컨테이너에 Spring 표준 환경 변수로 주입합니다.

```yaml
environment:
  - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
  - SPRING_DATASOURCE_USERNAME=${DB_USER}
  - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
  - SPRING_DATA_REDIS_HOST=redis
  - SPRING_DATA_REDIS_PORT=6379
  - SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}
```

컨테이너 간 통신은 Compose 기본 네트워크의 서비스명(`db`, `redis`)을 호스트명으로 사용하므로, 소스 코드에는 어떤 접속 정보도 하드코딩하지 않았습니다.

---

5. Docker Compose 서비스 실행
설정된 환경 변수와 Compose 파일 기반으로 서비스 일괄 구동

```Bash
# 전체 컨테이너 빌드 및 백그라운드 실행
docker compose up -d --build

# 실행 상태 확인
docker compose ps
```

</details>

- [x] 확인: MySQL과 Redis 컨테이너가 정상 기동되고, 애플리케이션이 두 저장소에 연결됩니다.

<details>
<summary><b>[자세히]</b></summary>

```Bash
## 컨테이너 상태 확인
docker compose ps
```

```Bash
NAME                      STATUS                   PORTS
expert-assignment-app     Up                       0.0.0.0:8080->8080/tcp
expert-assignment-mysql   Up (healthy)             0.0.0.0:3306->3306/tcp
expert-assignment-redis   Up (healthy)             0.0.0.0:6379->6379/tcp
```

---

```Bash
## docker의 MySQL에 접속
docker exec -it expert-assignment-mysql mysql -u root -p
```

```Bash
## 데이터베이스 목록 확인
show databases;
```

```Bash
+----------------------+
| Database             |
+----------------------+
| expert_assignment_db |
| information_schema   |
| mysql                |
| performance_schema   |
| sys                  |
+----------------------+
```

---

```Bash
## docker의 Redis에 접속
docker exec -it expert-assignment-redis redis-cli
```

```Bash
## 인증 및 읽기/쓰기 확인 (비밀번호는 .env의 REDIS_PASSWORD 값)
127.0.0.1:6379> AUTH <REDIS_PASSWORD>
OK
127.0.0.1:6379> SET lv1:check ok
OK
127.0.0.1:6379> GET lv1:check
"ok"
```

`requirepass`가 적용되어 있어 `AUTH` 없이 명령을 실행하면 `NOAUTH Authentication required.`가 반환됩니다.

---

```Bash
## 애플리케이션의 DB 커넥션 풀 기동 로그 확인
docker compose logs app | Select-String HikariPool
```

```Bash
HikariPool-1 - Starting...
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@7baf7e2c
HikariPool-1 - Start completed.
```

JDBC URL이 환경 변수로 주입된 값 그대로 기록되어, 하드코딩 없이 연결되었음을 확인할 수 있습니다.

```Bash
Database JDBC URL [jdbc:mysql://db:3306/expert_assignment_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul]
Database driver: MySQL Connector/J
Database version: 8.0.46
```

</details>

**Lv2. SQL을 JPA 인덱스로 표현하기**
- [x] SQL을 직접 실행하는 대신 `@Table`과 `@Index`로 인덱스를 선언합니다. 테이블 이름, 인덱스 이름과 컬럼 순서는 제공된 SQL과 같아야 합니다.

<details>
<summary><b>[자세히] </b></summary>

1. 요구된 SQL

```sql
CREATE INDEX idx_chat_world_created_at ON chat_messages(world_id, created_at);
```

---

2. 인덱스 선언 (`@Table` + `@Index`)
* SQL을 직접 실행하지 않고 엔티티 매핑으로 선언하여, 스키마가 엔티티 정의를 따라가도록 구성
* `columnList`의 컬럼 순서가 곧 복합 인덱스의 순서 — 요구된 SQL과 동일하게 `world_id`, `created_at` 순으로 지정

```java
@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(
                        name = "idx_chat_world_created_at",
                        columnList = "world_id, created_at"
                )
        }
)
public class ChatMessage {
```

`world_id`는 `@JoinColumn(name = "world_id")`, `created_at`은 `createdAt` 필드가 스프링 기본 네이밍 전략으로 매핑된 실제 컬럼명입니다.

 [ChatMessage.java 바로가기](./src/main/java/com/gameexpert/chat/entity/ChatMessage.java)

---

3. 스키마 자동 생성 설정
MySQL 등 외부 DB에서는 `spring.jpa.hibernate.ddl-auto` 기본값이 `none`이라 Hibernate가 스키마를 생성하지 않습니다.

엔티티에 선언한 인덱스가 실제 DB에 반영되도록 `update`로 설정했습니다.

```properties
spring.jpa.hibernate.ddl-auto=update
```

 [application.properties 바로가기](./src/main/resources/application.properties)

</details>

- [x] 확인: 서버를 실행하고 인덱스가 실제 DB에 생성됐는지 확인합니다.

<details>
<summary><b>[자세히]</b></summary>

```Bash
## docker의 MySQL에 접속
docker exec -it expert-assignment-mysql mysql -u root -p
```

```sql
USE expert_assignment_db;
SHOW INDEX FROM chat_messages;
```

```Bash
+---------------+------------+---------------------------+--------------+-------------+
| Table         | Non_unique | Key_name                  | Seq_in_index | Column_name |
+---------------+------------+---------------------------+--------------+-------------+
| chat_messages |          0 | PRIMARY                   |            1 | id          |
| chat_messages |          1 | idx_chat_world_created_at |            1 | world_id    |
| chat_messages |          1 | idx_chat_world_created_at |            2 | created_at  |
+---------------+------------+---------------------------+--------------+-------------+
```

`idx_chat_world_created_at`이 `Seq_in_index` 1 → `world_id`, 2 → `created_at` 순으로 생성되어 요구된 SQL과 일치합니다.

`Non_unique = 1`은 유니크 인덱스가 아니라는 의미로 `CREATE INDEX`와 동일하며, `PRIMARY`는 `@Id`가 생성한 기본키입니다.

</details>

- [x] 확인: 인덱스가 없으면 시작 검사에서 서버 실행을 중단합니다. `CHAT_HISTORY_INDEX_MISSING` 오류가 사라지고 서버가 정상 실행되어 `http://localhost:8080/`에서 첫 화면이 열립니다.

<details>
<summary><b>[자세히]</b></summary>

인덱스 선언 전에는 엔진의 시작 검사(`ChatHistoryIndexRequirement`)가 기동을 중단시켰습니다.

```Bash
Caused by: java.lang.IllegalStateException: CHAT_HISTORY_INDEX_MISSING:
chat_messages 테이블에 idx_chat_world_created_at(world_id, created_at) 인덱스가 필요합니다.
Lv 2의 @Table 인덱스 설정을 확인하세요.
```

---

인덱스 선언 후 정상 기동을 확인합니다.

```Bash
docker compose logs app --tail 30
```

```Bash
Tomcat started on port 8080 (http)
Started GameExpertApplication
```

---

`http://localhost:8080/` 접속 시 첫 화면이 정상적으로 열립니다.

![Lv2 첫 화면](./img/lv2_img.png)

</details>

**Lv3. 요청 검증과 DTO: 플레이어 등록**
- [x] API 명세에 맞게 플레이어 등록 Controller, 요청 DTO와 서비스를 구현합니다. 닉네임은 비어 있지 않은 2~12글자이며, 영문 대소문자와 숫자, 밑줄만 허용합니다.
- [x] 이미 등록된 닉네임이면 `ConflictException`으로 `DUPLICATE_NICKNAME` 에러를 던집니다.
- [x] Controller의 요청 매핑, JSON 본문 바인딩, DTO 검증과 성공 응답을 명세대로 구현합니다.
- [x] 중복이 아니면 제공된 `savePlayer(new Player(request.getNickname()))`로 저장합니다. 성공 응답은 본문 없는 `201`입니다.

<details>
<summary><b>[자세히] </b></summary>

1. 요청 DTO와 검증 규칙 (`CreatePlayerRequest`)
* 닉네임 제약을 Bean Validation 애노테이션으로 선언 — 검증 책임을 DTO에 두어 Controller와 Service는 검증을 신경 쓰지 않음

```java
@NotBlank
@Size(min = 2, max = 12)
@Pattern(regexp = "^[a-zA-Z0-9_]+$")
private final String nickname;
```

| 애노테이션 | 거르는 값 |
|---|---|
| `@NotBlank` | `null`, `""`, 공백만 있는 값 |
| `@Size(min = 2, max = 12)` | `"A"`, 13글자 이상 |
| `@Pattern` | `한글`, `ab cd`, `ab-cd`, `ab!` |

`@Size`와 `@Pattern`은 값이 `null`이면 검사를 건너뛰므로, `null`을 거르려면 `@NotBlank`가 반드시 필요합니다.

 [CreatePlayerRequest.java 바로가기](./src/main/java/com/gameexpert/player/dto/CreatePlayerRequest.java)

---

2. Controller (`PlayerController`)
* `@RequestBody`로 JSON 본문을 DTO에 바인딩
* `@Valid`로 DTO 검증을 수행 — 실패 시 서비스를 호출하지 않고 `400` 반환
* 성공 시 본문 없이 `201 Created` 반환

```java
@PostMapping("/players")
public ResponseEntity<Void> create(@Valid @RequestBody CreatePlayerRequest request) {
    playerService.createPlayer(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

`ResponseEntity.build()`는 본문 없이 상태 코드만 응답합니다.

 [PlayerController.java 바로가기](./src/main/java/com/gameexpert/player/controller/PlayerController.java)

---

3. Service (`PlayerService`)
* 저장 전 `existsByNickname`으로 중복을 확인하고, 중복이면 저장을 시도하지 않고 `ConflictException` 발생
* 동시 등록으로 제약 위반이 발생하는 경우는 제공된 `savePlayer`가 처리

```java
@Transactional
public void createPlayer(CreatePlayerRequest request) {
    if (playerRepository.existsByNickname(request.getNickname())) {
        throw new ConflictException("DUPLICATE_NICKNAME");
    }
    savePlayer(new Player(request.getNickname()));
}
```

두 검사는 서로 다른 상황을 담당합니다.

| 위치 | 담당하는 상황 |
|---|---|
| `existsByNickname` 사전 검사 | 이미 등록된 닉네임으로 들어온 일반적인 요청 |
| `savePlayer`의 `catch` | 두 요청이 동시에 사전 검사를 통과한 뒤 발생하는 제약 위반 |

사전 검사와 저장 사이에는 틈이 있어, 동시 요청은 양쪽 모두 "중복 없음"으로 판정될 수 있습니다. 이때 뒤늦은 INSERT가 유니크 제약에 걸리며, 이를 `catch`가 `409`로 변환합니다.

 [PlayerService.java 바로가기](./src/main/java/com/gameexpert/player/service/PlayerService.java)

</details>

- [x] 테스트 확인: `PlayerRegistrationTest.java`와 `PlayerApiTest.java`의 주석을 해제한 뒤 실행합니다.

<details>
<summary><b>[자세히]</b></summary>

```Bash
.\gradlew test
```

실행 결과는 Gradle이 생성하는 HTML 리포트에서 테스트별로 확인할 수 있습니다.

```Bash
build/reports/tests/test/index.html
```

</details>

- [x] 확인: 게임 화면에서 닉네임을 적용할 수 있습니다. 잘못된 닉네임이나 이미 등록된 닉네임은 새로 저장되지 않습니다.

<details>
<summary><b>[자세히]</b></summary>

`http://localhost:8080/`에서 닉네임을 입력하고 적용하면 등록이 완료되어 버튼이 `수정`으로 바뀌고 월드 목록이 표시됩니다.

![Lv3 닉네임 적용](./img/lv3_img.png)

</details>
