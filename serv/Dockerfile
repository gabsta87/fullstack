# ── Stage 1: build the jar ────────────────────────────────────────────────────
FROM gradle:8-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ping -c 3 google.com
RUN gradle bootJar --no-daemon --stacktrace --info

# ── Stage 2: run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Media storage directory (mounted as volume)
RUN mkdir -p /var/serv-media

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]