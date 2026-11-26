FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY gradlew .
COPY gradle/wrapper gradle/wrapper
COPY backend/build.gradle backend/settings.gradle backend/
COPY backend/identity-service/src backend/identity-service/src
COPY backend/report-service/src backend/report-service/src
ARG SERVICE
RUN sed -i 's/\r$//' gradlew && sh gradlew --no-daemon -p backend ":${SERVICE}:bootJar" && cp backend/"${SERVICE}"/build/libs/*.jar /service.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /service.jar service.jar
ENTRYPOINT ["java", "-jar", "service.jar"]
