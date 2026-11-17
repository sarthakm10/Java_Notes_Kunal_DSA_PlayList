# syntax=docker/dockerfile:1
# Multi-stage build: a JDK image builds the jar; a smaller JRE image runs it.

# ---------- stage 1: build the fat jar, then split it into layers ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# A Windows checkout (core.autocrlf=true) can turn mvnw into CRLF and drop its exec bit: repair both.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
 && ./mvnw -q -B dependency:go-offline                        # cached until pom.xml changes
COPY src/ src/
RUN ./mvnw -q -B -DskipTests package \
 && java -Djarmode=tools -jar target/task-tracker-0.16.0.jar extract --layers --destination /layers
#    /layers/dependencies/lib/*.jar   /layers/application/task-tracker-0.16.0.jar   (+ 2 empty layers here)

# ---------- stage 2: the runtime image ----------
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home app        # never run as root
WORKDIR /app
# Least-changing layer first: a code-only change rebuilds and pushes just the last (small) layer.
COPY --from=build /layers/dependencies/          ./
COPY --from=build /layers/spring-boot-loader/    ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/           ./
USER 10001
EXPOSE 8080
# Size the heap from the container's memory limit, not the host's RAM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75" \
    SPRING_PROFILES_ACTIVE=prod
# Exec form: java is PID 1 and receives SIGTERM directly, so graceful shutdown runs.
# Started from WORKDIR as "java -jar <jar>": the form whose classes a CDS archive can cover (see the notes).
ENTRYPOINT ["java", "-jar", "task-tracker-0.16.0.jar"]
