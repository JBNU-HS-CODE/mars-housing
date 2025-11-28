# 1단계: 빌드 단계 (Build Stage)
FROM eclipse-temurin:21-jdk-jammy AS builder 
LABEL authors="astar"

COPY . /app
WORKDIR /app

RUN chmod +x gradlew

RUN ./gradlew clean bootJar --no-daemon

# --- 분리선 ---

# 2단계: 최종 실행 단계 (Run Stage) - 경량화된 JRE 이미지 사용
FROM eclipse-temurin:21-jre-jammy
LABEL org.name="astar"

COPY --from=builder /app/build/libs/mars-housing-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]