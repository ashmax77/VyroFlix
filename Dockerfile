# ============================================================
# VyroFlix Platform — Multi-Stage Production Dockerfile for Render
# Builds combined platform fat JAR and runs on Eclipse Temurin 21 Alpine
# ============================================================

# ------------------------------------------------------------
# Stage 1: Build JAR from source
# ------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven POMs first for dependency layer caching
COPY services/pom.xml ./services/pom.xml
COPY services/common/pom.xml ./services/common/pom.xml
COPY services/identity-service/pom.xml ./services/identity-service/pom.xml
COPY services/content-service/pom.xml ./services/content-service/pom.xml
COPY services/video-service/pom.xml ./services/video-service/pom.xml
COPY services/encoding-service/pom.xml ./services/encoding-service/pom.xml
COPY services/streaming-service/pom.xml ./services/streaming-service/pom.xml
COPY services/history-service/pom.xml ./services/history-service/pom.xml
COPY services/search-service/pom.xml ./services/search-service/pom.xml
COPY services/recommendation-service/pom.xml ./services/recommendation-service/pom.xml
COPY services/api-gateway/pom.xml ./services/api-gateway/pom.xml
COPY services/platform/pom.xml ./services/platform/pom.xml

# Copy all Java source files and resources
COPY services/ ./services/

# Package platform fat JAR (skipping unit tests to accelerate build)
RUN mvn clean package -pl :platform -am -DskipTests -f services/pom.xml

# ------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# ------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="VyroFlix Team"
LABEL description="VyroFlix Combined Platform Backend"

# Run as non-root user for security
RUN addgroup -S vyroflix && adduser -S vyroflix -G vyroflix
USER vyroflix:vyroflix

WORKDIR /app

# Copy the built fat JAR from builder stage
COPY --from=builder /build/services/platform/target/platform-*.jar /app/app.jar

ENV PORT=8081
EXPOSE 8081

# Health check against actuator endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

# Tuned for Render Free 512MB RAM ceiling:
# - UseSerialGC minimizes GC heap/thread overhead compared to G1GC
# - -Xmx384m reserves 128MB headroom for JVM native memory, metaspace, and OS
ENTRYPOINT ["java", \
  "-Xmx384m", \
  "-XX:+UseSerialGC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=render", \
  "-jar", "/app/app.jar"]
