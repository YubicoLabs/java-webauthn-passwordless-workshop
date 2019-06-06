FROM alpine/git as clone 
WORKDIR /app
RUN git clone https://github.com/YubicoLabs/java-webauthn-passwordless-workshop.git

FROM maven:3.5-jdk-8-alpine as build 
WORKDIR /app
COPY --from=clone /app/java-webauthn-passwordless-workshop/2_Credential_Repository/complete /app 
RUN mvn clean package

FROM openjdk:8-jre-alpine
VOLUME /tmp
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar /app
ENTRYPOINT ["java","-jar","/app/demo-0.0.1-SNAPSHOT.jar"]
