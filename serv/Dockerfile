# ── Stage 1: build the jar ────────────────────────────────────────────────────
FROM gradle:8-jdk21 AS builder

RUN echo "nameserver 1.1.1.1" > /etc/resolv.conf
RUN echo "nameserver 8.8.8.8" >> /etc/resolv.conf

WORKDIR /app
COPY . .
USER root

ENV GRADLE_OPTS="-Xmx512m -XX:MaxMetaspaceSize=384m"

RUN gradle bootJar --no-daemon --no-build-cache

# ── Stage 2: run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Media storage directory (mounted as volume)
RUN mkdir -p /var/serv-media

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]