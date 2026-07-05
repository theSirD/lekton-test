FROM gradle:8.11.1-jdk21 AS build
WORKDIR /app
COPY gradle.properties settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY catalog-service ./catalog-service
RUN gradle :catalog-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/catalog-service/build/libs/catalog-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
