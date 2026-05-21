FROM gradle:8.13-jdk17 AS builder
WORKDIR /workspace

COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle
COPY src ./src

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/data_treasure.jar /app/data_treasure.jar

ENTRYPOINT ["java", "-jar", "/app/data_treasure.jar"]
