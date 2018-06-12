FROM openjdk:8-jre-alpine3.7

RUN ls -al .
ADD heimdal.jar .

ENTRYPOINT java -jar heimdall.jar