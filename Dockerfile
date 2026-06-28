# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build
COPY pom.xml .
# Download dependencies first (layer cache)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S bulletbeats && adduser -S bulletbeats -G bulletbeats

# Copy jar
COPY --from=builder /build/target/*.jar app.jar

RUN chown -R bulletbeats:bulletbeats /app

USER bulletbeats

EXPOSE 8080

# -XX:+UseContainerSupport    — respects Docker memory limits
# -XX:MaxRAMPercentage=75     — use 75% of container memory for heap
# -Djava.security.egd         — faster startup for Tomcat
# -Djava.net.preferIPv4Stack  — Docker bridge has no IPv6; Supabase host resolves to IPv6 first
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Djava.net.preferIPv4Stack=true", \
  "-jar", "app.jar"]
