# 멀티스테이지 빌드 — 빌드 이미지(JDK)와 런타임 이미지(JRE) 분리해서 최종 이미지 크기 축소.

# === Stage 1: 빌드 ===
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Gradle wrapper + 설정 파일 먼저 복사 → dependency 캐시 레이어 활용 (소스만 바뀌면 deps 재다운로드 안 함)
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 jar 빌드
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# === Stage 2: 런타임 ===
FROM eclipse-temurin:17-jre
WORKDIR /app

# 빌드 산출물만 복사 (builder 이미지는 버려짐)
COPY --from=builder /app/build/libs/*.jar app.jar

# Render는 PORT env로 동적 포트 주입. Spring이 server.port=${PORT:8080}로 처리.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
