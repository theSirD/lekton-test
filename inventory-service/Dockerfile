FROM gradle:8.11.1-jdk21 AS build
WORKDIR /app
COPY gradle.properties settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY inventory-service ./inventory-service
RUN gradle :inventory-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/inventory-service/build/libs/inventory-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
