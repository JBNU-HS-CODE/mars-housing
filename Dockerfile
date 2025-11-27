# 1단계: 빌드 단계 (Build Stage)
FROM eclipse-temurin:21-jdk-jammy AS builder 
LABEL authors="astar"

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean build --no-daemon

# --- 분리선 ---

# 2단계: 최종 실행 단계 (Run Stage) - 경량화된 JRE 이미지 사용
FROM eclipse-temurin:21-jre-jammy
LABEL org.name="astar"

# 빌더 단계에서 생성된 JAR 파일을 복사
COPY --from=builder /home/gradle/src/build/libs/mars-housing-0.0.1-SNAPSHOT-all.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]