FROM openjdk:8-jre-alpine3.7

ADD ./Dockerfile .
#ADD ./build/libs/heimdall-0.0.1-SNAPSHOT.jar .
ADD ./heimdall.jar .
# RUN cat ./Dockerfile
# ADD ./build/libs/heimdall-0.0.1-SNAPSHOT.jar .

ENTRYPOINT java -jar heimdall.jar
#CMD cat Dockerfile