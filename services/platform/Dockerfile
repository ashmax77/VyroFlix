# ============================================================
# VyroFlix Platform — Dockerfile (Render / Production Deployment)
# Multi-stage build with Eclipse Temurin Java 21
# ============================================================

FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="VyroFlix Team"
LABEL description="VyroFlix Combined Platform Backend"

# Run as non-root user for security
RUN addgroup -S vyroflix && adduser -S vyroflix -G vyroflix
USER vyroflix:vyroflix

WORKDIR /app

# Copy the built fat JAR from target directory
ARG JAR_FILE=target/platform-*.jar
COPY ${JAR_FILE} app.jar

ENV PORT=8081
EXPOSE 8081

# Health check against actuator endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
