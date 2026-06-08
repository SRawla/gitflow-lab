FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S tbs && adduser -S tbs -G tbs
USER tbs

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]