FROM openjdk:8-jre-alpine3.7

ADD ./build/libs/heimdall-latest.jar .

ENTRYPOINT java -jar heimdall-latest.jar