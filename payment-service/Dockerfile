# Build JARs locally first: ./gradlew bootJar -x test
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY payment-service/build/libs/payment-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
