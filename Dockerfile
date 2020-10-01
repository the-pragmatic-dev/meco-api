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

ENV SPRING_PROFILES_ACTIVE=prod
ENV GEOLITE_DIRECTORY=/meco/geodb

ARG DEPENDENCY=/workspace/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /meco/lib
COPY --from=build ${DEPENDENCY}/META-INF /meco/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /meco

RUN addgroup -S meco && adduser -S meco -G meco
RUN mkdir /meco/geodb
RUN chown -R meco:meco /meco

USER meco:meco

ENTRYPOINT ["java","-cp","meco:meco/lib/*","uk.thepragmaticdev.Application"]