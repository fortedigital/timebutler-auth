FROM gcr.io/distroless/java17-debian11

WORKDIR /app

COPY build/libs/link.timebutler.timebutler-auth-all.jar app.jar

CMD ["app.jar"]