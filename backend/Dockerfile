FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
# 1. FIX: Explicitly call 'shadowJar' instead of 'build'
RUN gradle shadowJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# 2. FIX: Explicitly grab the '-all.jar' (which is the Fat Jar with the manifest)
COPY --from=builder /app/build/libs/*-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]