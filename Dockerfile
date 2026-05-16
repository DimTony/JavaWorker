# ── Stage 1: build ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q          # cache deps layer
COPY src ./src
RUN mvn package -q -DskipTests

# ── Stage 2: runtime ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/redis-worker-1.0.0.jar app.jar

# Non-root user for security
RUN addgroup -S worker && adduser -S worker -G worker
USER worker

ENTRYPOINT ["java", "-jar", "app.jar"]