FROM openjdk:8-jre-alpine3.7

ADD ./heimdall.jar .

ENTRYPOINT java -jar heimdall.jar