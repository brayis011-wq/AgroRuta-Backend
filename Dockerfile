# ============================================================
# BACKEND - Spring Boot (Maven + Java 21)
# ============================================================

# STAGE 1: Compilar con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# STAGE 2: Imagen final ligera solo con el JAR
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S agroruta && adduser -S agroruta -G agroruta
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown agroruta:agroruta app.jar
USER agroruta
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
