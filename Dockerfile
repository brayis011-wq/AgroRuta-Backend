# BACKEND - Spring Boot (Maven + Java 21)

# STAGE 1: Compilar con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# STAGE 2: Imagen final con el JAR y el cliente de MySQL
# Base Ubuntu (no Alpine) para tener el cliente oficial de MySQL 8:
# en Alpine solo existe el de MariaDB, que genera dumps que TiDB no siempre acepta.
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends mysql-client wget ca-certificates \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system agroruta && useradd --system --gid agroruta --create-home agroruta
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Crear logs y backups y asignar todo a agroruta ANTES de cambiar de usuario
RUN mkdir -p /app/logs /app/backups && chown -R agroruta:agroruta /app

USER agroruta
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]