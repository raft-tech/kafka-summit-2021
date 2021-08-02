FROM maven:3.8.1-openjdk-11-slim

COPY ./ ./

RUN mvn clean install

CMD ["java", "-jar", "target/kafka-streams-message-security-1.0.0.jar"]