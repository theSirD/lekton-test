# Build JARs locally first: ./gradlew bootJar -x test
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY inventory-service/build/libs/inventory-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
