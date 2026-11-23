# Build from the repository root:  docker build -f services/audit-service/Dockerfile -t ssp-audit-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -q -pl services/audit-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/services/audit-service/target/audit-service.jar app.jar
USER app
EXPOSE 8084
ENTRYPOINT ["java","-jar","app.jar"]
