# ============================================================
# VyroFlix Platform — Dockerfile (Render / Production Deployment)
# Multi-stage build with Eclipse Temurin Java 21
# ============================================================

# Stage 1: Build JAR from repository root or services directory
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml ./pom.xml
COPY common/ ./common/
COPY identity-service/ ./identity-service/
COPY content-service/ ./content-service/
COPY video-service/ ./video-service/
COPY encoding-service/ ./encoding-service/
COPY streaming-service/ ./streaming-service/
COPY history-service/ ./history-service/
COPY search-service/ ./search-service/
COPY recommendation-service/ ./recommendation-service/
COPY api-gateway/ ./api-gateway/
COPY platform/ ./platform/

RUN mvn clean package -pl :platform -am -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="VyroFlix Team"
LABEL description="VyroFlix Combined Platform Backend"

RUN addgroup -S vyroflix && adduser -S vyroflix -G vyroflix
USER vyroflix:vyroflix

WORKDIR /app

COPY --from=builder /build/platform/target/platform-*.jar app.jar

ENV PORT=8081
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xmx384m", \
  "-XX:+UseSerialGC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=render", \
  "-jar", "app.jar"]
