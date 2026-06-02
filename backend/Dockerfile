# ── Stage 1 : Build Maven ──────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Cache des dépendances Maven (layer séparée)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Build de l'application
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2 : Runtime léger ────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Sécurité : utilisateur non-root
RUN addgroup -S skillmap && adduser -S skillmap -G skillmap

# Copie uniquement le JAR final
COPY --from=builder /app/target/*.jar app.jar

# Ownership au user non-root
RUN chown skillmap:skillmap app.jar

USER skillmap

EXPOSE 8080

# Health check Docker natif
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java",
    "-XX:+UseVirtualThreads",
    "-XX:MaxRAMPercentage=75.0",
    "-XX:+ExitOnOutOfMemoryError",
    "-Djava.security.egd=file:/dev/./urandom",
    "-jar", "app.jar"]
