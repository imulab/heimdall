FROM openjdk:8-jre-alpine3.7

ADD /app/heimdal.jar .

ENTRYPOINT java -jar heimdall.jar