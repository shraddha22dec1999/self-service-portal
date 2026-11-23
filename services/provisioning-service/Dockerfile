# Build from the repository root:  docker build -f services/provisioning-service/Dockerfile -t ssp-provisioning-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -q -pl services/provisioning-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/services/provisioning-service/target/provisioning-service.jar app.jar
USER app
EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar"]
