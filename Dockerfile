FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY backend backend
COPY frontend frontend
RUN mvn -B -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/backend/target/telecom-dashboard-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
