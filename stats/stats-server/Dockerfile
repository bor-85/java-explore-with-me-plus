FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
RUN mvn -pl stats/stats-server -am clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/stats/stats-server/target/*.jar app.jar
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
