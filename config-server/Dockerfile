FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace

WORKDIR /workspace/config-server
COPY config-server/pom.xml .
COPY config-server/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

ENV JAVA_OPTS=""

WORKDIR /app

RUN apk add --no-cache tzdata

COPY --from=builder /workspace/config-server/target/*.jar /app/app.jar

EXPOSE 8888

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
