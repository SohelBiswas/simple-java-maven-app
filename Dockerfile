FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home appuser
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
