# ─── Stage 1: Build the React/TypeScript frontend ───────────────────────────
FROM node:22-alpine AS frontend-build

WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


# ─── Stage 2: Build the Spring Boot backend (JAR) ───────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS backend-build

WORKDIR /app/backend

# curl is required by the Maven wrapper to download Maven on first run
RUN apk add --no-cache curl

# Copy Maven wrapper and POM first so dependency layer is cached
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source code
COPY backend/src ./src

# Copy the compiled frontend assets into Spring Boot's static resources directory
# so the JAR will serve them at the root path
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static

# Package the JAR, skip tests (already verified)
RUN ./mvnw package -DskipTests -q


# ─── Stage 3: Minimal runtime image ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=backend-build /app/backend/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
