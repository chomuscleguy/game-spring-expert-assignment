# ---------- 1단계: 빌드 스테이지 ----------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# 의존성 관련 파일 먼저 복사 → 소스만 바뀌면 이 레이어는 캐시 재사용
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Windows(CRLF) → Linux(LF) 변환 + 실행 권한
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 의존성 미리 받아두기 (실패해도 빌드는 계속)
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ---------- 2단계: 실행 스테이지 ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/build/libs/*.jar /app/myapp.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/myapp.jar"]