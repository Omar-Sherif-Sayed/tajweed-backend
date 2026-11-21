# The production image of tajweed-backend: built with JDK 25, run on a JRE 25, as a user without privileges.
# Build:  docker build -t tajweed-backend .
# Run:    docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=... -e SPRING_DATASOURCE_USERNAME=... \
#                    -e SPRING_DATASOURCE_PASSWORD=... -e CORS_ORIGINS=... tajweed-backend
# The settings and their environment variables are listed in README.md. No secret is ever baked into the image.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY config config
COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/tajweed-backend-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 tajweed
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER tajweed
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
