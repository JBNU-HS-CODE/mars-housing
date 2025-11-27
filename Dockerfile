FROM ubuntu:latest
LABEL authors="astar"
FROM eclipse-temurin:21-jdk-jammy AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon
LABEL org.name="astar"
COPY --from=build /home/gradle/src/build/libs/mars-housing-0.0.1-SNAPSHOT-all.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
