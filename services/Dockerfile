# ============================================================
# VyroFlix Platform — Multi-Stage Production Dockerfile for Render
# Builds combined platform fat JAR from within services/
# ============================================================

# Stage 1: Build JAR from Maven Reactor
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven POMs first for dependency layer caching
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY identity-service/pom.xml ./identity-service/pom.xml
COPY content-service/pom.xml ./content-service/pom.xml
COPY video-service/pom.xml ./video-service/pom.xml
COPY encoding-service/pom.xml ./encoding-service/pom.xml
COPY streaming-service/pom.xml ./streaming-service/pom.xml
COPY history-service/pom.xml ./history-service/pom.xml
COPY search-service/pom.xml ./search-service/pom.xml
COPY recommendation-service/pom.xml ./recommendation-service/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY platform/pom.xml ./platform/pom.xml

# Copy all Java source files and resources
COPY . ./

# Package platform fat JAR (skipping unit tests to accelerate build)
RUN mvn clean package -pl :platform -am -DskipTests -f pom.xml && \
    cp $(find . -name "platform-*-exec.jar" | head -n 1) /build/app.jar

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="VyroFlix Team"
LABEL description="VyroFlix Combined Platform Backend"

# Run as non-root user for security
RUN addgroup -S vyroflix && adduser -S vyroflix -G vyroflix
USER vyroflix:vyroflix

WORKDIR /app

# Copy the built fat executable JAR from builder stage
COPY --from=builder /build/app.jar /app/app.jar

ENV PORT=8081
EXPOSE 8081

# Health check against actuator endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

# Tuned for Render Free 512MB RAM ceiling:
# - UseSerialGC minimizes GC heap/thread overhead compared to G1GC
# - -Xmx384m reserves 128MB headroom for JVM native memory, metaspace, and OS
ENTRYPOINT ["java", \
  "-Xms64m", \
  "-Xmx320m", \
  "-Xss512k", \
  "-XX:MaxMetaspaceSize=160m", \
  "-XX:ReservedCodeCacheSize=48m", \
  "-XX:CICompilerCount=2", \
  "-XX:+UseSerialGC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Dspring.jmx.enabled=false", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=render", \
  "-jar", "/app/app.jar"]
