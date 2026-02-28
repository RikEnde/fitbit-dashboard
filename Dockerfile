# syntax=docker/dockerfile:1

# Stage 1: Build the SvelteKit dashboard
FROM node:20-alpine AS dashboard-build
WORKDIR /app/dashboard
COPY dashboard/package.json dashboard/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY dashboard/ ./
RUN npm run build

# Stage 2: Build the Spring Boot JARs
FROM maven:3.9-eclipse-temurin-25 AS java-build
WORKDIR /build

# Copy all poms and source
COPY pom.xml ./
COPY model/ model/
COPY importer/ importer/
COPY importer-cli/ importer-cli/
COPY server/ server/

# Copy dashboard static files into server resources
COPY --from=dashboard-build /app/dashboard/build/ server/src/main/resources/static/

# Build all modules — Maven local repo is cached via BuildKit mount
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl model,importer,importer-cli,server clean package -DskipTests --no-transfer-progress

# Stage 3: Runtime image
FROM eclipse-temurin:25-jre-noble

# Install PostgreSQL 17
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    gnupg \
    lsb-release \
    sudo \
    && curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | gpg --dearmor -o /usr/share/keyrings/postgresql.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/postgresql.gpg] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" \
        > /etc/apt/sources.list.d/pgdg.list \
    && apt-get update && apt-get install -y --no-install-recommends postgresql-17 \
    && rm -rf /var/lib/apt/lists/*

# Allow the app to run pg_ctl as postgres user without a password
RUN echo "root ALL=(postgres) NOPASSWD: ALL" >> /etc/sudoers

# Copy application JARs
COPY --from=java-build /build/server/target/fitbit-server-0.0.1-SNAPSHOT.jar /app/server.jar
COPY --from=java-build /build/importer-cli/target/fitbit-importer-cli-0.0.1-SNAPSHOT.jar /app/importer-cli.jar

# Copy entrypoint
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

VOLUME /var/lib/postgresql/data

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
