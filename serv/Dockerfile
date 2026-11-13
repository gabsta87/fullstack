# ── Stage 1: build the jar ────────────────────────────────────────────────────
FROM gradle:8-jdk21 AS builder
WORKDIR /app
COPY . .
USER root

ENV GRADLE_OPTS="-Xmx512m -XX:MaxMetaspaceSize=384m"

RUN curl -I https://plugins.gradle.org/m2/org/springframework/boot/org.springframework.boot.gradle.plugin/3.3.4/org.springframework.boot.gradle.plugin-3.3.4.pom

RUN gradle bootJar --no-daemon --no-build-cache

# ── Stage 2: run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Media storage directory (mounted as volume)
RUN mkdir -p /var/serv-media

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]