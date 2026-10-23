# ==============================================================================
# TRUSTVAULT MULTI-STAGE DOCKERFILE
# Stage 1: Build with Eclipse Temurin JDK 21 and Maven
# Stage 2: Hardened, minimal JRE 21 runtime container
# ==============================================================================

# STAGE 1: BUILD STAGE
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven descriptor and fetch dependencies first for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy application source code
COPY src ./src
COPY database ./database

# Package executable Spring Boot fat JAR
RUN mvn clean package -DskipTests -B

# ==============================================================================
# STAGE 2: PRODUCTION RUNTIME
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

# Security hardening: Create dedicated unprivileged service account
RUN addgroup -S trustvault && adduser -S trustvault -G trustvault

WORKDIR /app

# Copy artifact from builder stage
COPY --from=builder /build/target/trustvault-1.0.0.jar /app/trustvault.jar

# Set ownership
RUN chown -R trustvault:trustvault /app

# Switch to non-root user
USER trustvault

# Environment defaults (Render sets $PORT dynamically)
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:${PORT}/api/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/trustvault.jar"]
