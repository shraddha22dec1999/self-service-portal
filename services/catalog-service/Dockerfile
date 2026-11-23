# Build from the repository root:  docker build -f services/catalog-service/Dockerfile -t ssp-catalog-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -q -pl services/catalog-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/services/catalog-service/target/catalog-service.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
