# ── Stage 1: build the jar ────────────────────────────────────────────────────
FROM gradle:8-jdk21 AS builder
WORKDIR /app
COPY . .
USER root
RUN apt-get update && apt-get install -y curl
RUN curl -I https://plugins.gradle.org
RUN curl -I https://repo.spring.io/release/org/springframework/boot/org.springframework.boot.gradle.plugin/3.3.4/org.springframework.boot.gradle.plugin-3.3.4.pom
RUN gradle bootJar --no-daemon --stacktrace --info

# ── Stage 2: run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Media storage directory (mounted as volume)
RUN mkdir -p /var/serv-media

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]