FROM openjdk:8-jre-alpine3.7

RUN ls -al .
ADD ./build/heimdal.jar .

ENTRYPOINT java -jar heimdall.jar