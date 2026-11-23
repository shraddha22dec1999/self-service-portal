# Build from the repository root:  docker build -f services/notification-service/Dockerfile -t ssp-notification-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -q -pl services/notification-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/services/notification-service/target/notification-service.jar app.jar
USER app
EXPOSE 8085
ENTRYPOINT ["java","-jar","app.jar"]
