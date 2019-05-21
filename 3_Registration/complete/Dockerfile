FROM alpine/git as clone 
WORKDIR /app
RUN git clone https://github.com/elukewalker/PasswordlessWorkshop.git

FROM maven:3.5-jdk-8-alpine as build 
WORKDIR /app
COPY --from=clone /app/PasswordlessWorkshop/3_Registration/complete /app 
RUN mvn clean package

FROM openjdk:8-jre-alpine
VOLUME /tmp
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar /app
ENTRYPOINT ["java","-jar","/app/demo-0.0.1-SNAPSHOT.jar"]
