FROM gradle:7.6-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]