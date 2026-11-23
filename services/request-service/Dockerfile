# Build from the repository root:  docker build -f services/request-service/Dockerfile -t ssp-request-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -q -pl services/request-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/services/request-service/target/request-service.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]
