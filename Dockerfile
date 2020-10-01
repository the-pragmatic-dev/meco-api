FROM openjdk:13-jdk-alpine

ARG LIBS=/var/lib/meco

RUN addgroup -S meco && adduser -S meco -G meco
RUN mkdir ${LIBS}
RUN chmod 755 ${LIBS}
RUN chown -R meco:meco ${LIBS}

COPY target/*-exec.jar meco.jar
RUN chown meco:meco meco.jar

USER meco:meco

ENTRYPOINT ["java","-jar","/meco.jar"]