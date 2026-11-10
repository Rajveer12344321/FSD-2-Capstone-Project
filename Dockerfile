# Stage 1: build the React front end
FROM node:22-alpine AS frontend
WORKDIR /fe
COPY frontend/package.json ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# Stage 2: build the Spring Boot back end, bundling the React build as static resources
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /be
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /fe/dist ./src/main/resources/static
RUN mvn -q -B -DskipTests package

# Stage 3: runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /be/target/bloodbridge-1.0.0.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
