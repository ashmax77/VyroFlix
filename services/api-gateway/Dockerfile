# ============================================================
# VyroFlix API Gateway — Dockerfile
# Multi-stage build with Eclipse Temurin Java 21
# ============================================================

# Stage 1: Runtime base
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="VyroFlix Team"
LABEL description="VyroFlix API Gateway Service"

# Run as non-root user for security
RUN addgroup -S vyroflix && adduser -S vyroflix -G vyroflix
USER vyroflix:vyroflix

WORKDIR /app

# Copy the built JAR from target directory
ARG JAR_FILE=target/api-gateway-*.jar
COPY ${JAR_FILE} app.jar

ENV PORT=8080
EXPOSE 8080

# Health check against actuator endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
