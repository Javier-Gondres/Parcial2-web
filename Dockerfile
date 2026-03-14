FROM gradle:9.2.0-jdk17 AS build
WORKDIR /workspace

COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /var/lib/parcial2

COPY --from=build /workspace/build/install/parcial2 /opt/parcial2

EXPOSE 7000
EXPOSE 9092

CMD ["sh", "-c", "mkdir -p /var/lib/parcial2/data && /opt/parcial2/bin/parcial2"]
