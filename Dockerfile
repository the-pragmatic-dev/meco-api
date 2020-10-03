#
# Build stage
#
FROM openjdk:13-jdk-alpine AS build

WORKDIR /workspace

ARG GEOLITE_URL=${GEOLITE_URL}
RUN echo ${GEOLITE_URL}

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY pom.xml .
COPY src src

RUN ./mvnw install -P docker
RUN mkdir -p target/dependency/geolite && (cd target/dependency; jar -xf ../*-exec.jar)
RUN wget -c ${GEOLITE_URL} -O - | tar -xz -C  target/dependency/geolite

#
# Package stage
#
FROM openjdk:13-jdk-alpine

ENV SPRING_PROFILES_ACTIVE=prod
ENV GEOLITE_DIRECTORY=/meco/geolite/

ARG DEPENDENCY=/workspace/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /meco/lib
COPY --from=build ${DEPENDENCY}/META-INF /meco/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /meco
COPY --from=build ${DEPENDENCY}/geolite /meco/geolite

RUN addgroup -S meco && adduser -S meco -G meco
RUN mkdir /meco/logs
RUN chown -R meco:meco /meco

USER meco:meco

EXPOSE 8080

ENTRYPOINT ["java","-cp","meco:meco/lib/*","uk.thepragmaticdev.Application"]
