# ── Stage 1: build the jar ────────────────────────────────────────────────────
# 8.14 line, multi-arch (amd64 NAS + arm64 Mac); patch versions of the tag lag behind on arm64
FROM gradle:8.14-jdk21 AS builder
WORKDIR /app
COPY . .
USER root

ENV GRADLE_OPTS="-Xmx512m -XX:MaxMetaspaceSize=384m"

RUN gradle bootJar --no-daemon --no-build-cache

# ── Stage 2: run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
# ffmpeg re-encodes uploaded videos and extracts their thumbnails (VideoProcessingService)
RUN apk add --no-cache ffmpeg
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Media storage directory (mounted as volume)
RUN mkdir -p /var/serv-media /var/serv-private

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]