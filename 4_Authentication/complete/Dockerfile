FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=target/demo-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} demo.jar
ENTRYPOINT ["java","-jar","/demo.jar"]