# The JAR must be built before this image is built:
#   ./gradlew bootJar -x test
#   docker build -t spring-xpose-sample-rest .
#
# In CI the Gradle build runs on the runner (where ../spring-xpose is available
# via the composite build), then the pre-built JAR is copied into this image.

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

