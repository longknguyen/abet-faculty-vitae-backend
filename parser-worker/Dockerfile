FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .
RUN ./gradlew :parser-worker:bootJar --no-daemon

FROM eclipse-temurin:21-jre

RUN groupadd --system parser-worker \
    && useradd --system --gid parser-worker --home-dir /nonexistent parser-worker

WORKDIR /app
COPY --from=build /workspace/parser-worker/build/libs/parser-worker-0.0.1-SNAPSHOT.jar parser-worker.jar

USER parser-worker:parser-worker
EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:MaxMetaspaceSize=128m", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/parser-worker.jar"]
