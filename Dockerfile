FROM openjdk:8-jdk-alpine
EXPOSE 8080
VOLUME /tmp
#ARG JAR_FILE
#COPY ${JAR_FILE} app.jar

#ADD /target/uaxctf.jar app.jar
ADD target/uaxctf-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]