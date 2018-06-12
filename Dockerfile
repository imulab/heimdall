FROM openjdk:8-jre-alpine3.7

RUN ls -al .
RUN cat Dockerfile
# ADD ./build/libs/heimdall-0.0.1-SNAPSHOT.jar .

ENTRYPOINT java -jar heimdall-0.0.1-SNAPSHOT.jar