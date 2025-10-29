FROM openjdk:21-jdk
WORKDIR /app
COPY target/AIDemo-0.0.1-SNAPSHOT.jar /app/AIDemo.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/AIDemo.jar"]


