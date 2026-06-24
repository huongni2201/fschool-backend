# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8080

COPY --from=build /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
