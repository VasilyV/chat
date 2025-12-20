# ---------- BUILD STAGE ----------
FROM maven:3.9.5-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn -q -DskipTests clean package

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]