# ─── Runtime image ───────────────────────────────────────────────────────────
# Uses a locally available image that contains Eclipse Temurin JRE 21.
# The frontend and backend are pre-built on the host before running
# `docker compose build` (node:22 + Maven offline build).
FROM orbital-app:latest

WORKDIR /app

# Override the pre-existing app.jar in the base image with our Spring Boot fat-JAR
# (which already embeds the compiled React frontend under BOOT-INF/classes/static)
COPY backend/target/*.jar app.jar

EXPOSE 8081

# The base image's /__cacert_entrypoint.sh handles JVM trust-store setup then
# exec's the supplied command — this is the standard eclipse-temurin pattern.
ENTRYPOINT ["/__cacert_entrypoint.sh", "java", "-jar", "app.jar"]
