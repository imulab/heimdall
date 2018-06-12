FROM openjdk:8-jre-alpine3.7

ADD ./Dockerfile .
ADD ./build/libs/heimdall-0.0.1-SNAPSHOT.jar .
# RUN cat ./Dockerfile
# ADD ./build/libs/heimdall-0.0.1-SNAPSHOT.jar .

ENTRYPOINT java -jar heimdall-0.0.1-SNAPSHOT.jar
#CMD cat Dockerfile