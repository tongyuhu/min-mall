FROM eclipse-temurin:21-jre-jammy@sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269
WORKDIR /app
COPY mall-app/target/mall-app-1.0.0-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
