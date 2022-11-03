FROM azul/zulu-openjdk-debian:17-latest AS builder

WORKDIR /app

COPY . .

RUN ./gradlew build

FROM azul/zulu-openjdk-debian:17-jre-latest

VOLUME /app

COPY --from=builder /app/build/resources /app/resources

COPY --from=builder /app/build/libs /app/libs

COPY --from=builder /app/build/classes /app/classes

ENTRYPOINT [ "java", "-jar", "/app/libs/gh-metrics-collector.jar" ]