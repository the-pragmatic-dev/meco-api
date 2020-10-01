#
# Build stage
#
FROM openjdk:13-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY pom.xml .
COPY src src

RUN ./mvnw install -P docker
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*-exec.jar)

#
# Package stage
#
FROM openjdk:13-jdk-alpine
ARG DEPENDENCY=/workspace/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","uk.thepragmaticdev.Application"]
