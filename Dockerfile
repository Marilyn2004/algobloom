# One free Render web service serves both the SPA and API from the same origin.
FROM node:22-bookworm-slim AS frontend
WORKDIR /frontend
RUN npm install --global pnpm@10.11.0
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile --ignore-scripts
COPY frontend/ ./
RUN pnpm build

FROM maven:3.9.11-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src src
COPY --from=frontend /frontend/dist/ src/main/resources/static/
# Build-time tests run separately against isolated local PostgreSQL, never a cloud datasource.
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=backend /app/target/leetcode-review-system-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=staging
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:ActiveProcessorCount=2"
EXPOSE 10000
ENTRYPOINT ["java","-jar","/app/app.jar"]
