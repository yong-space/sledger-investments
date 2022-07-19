FROM openjdk:18-slim-bullseye
WORKDIR /build
COPY ./build/libs/*.jar app.jar
RUN jar -xf app.jar
RUN jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    -q \
    --recursive \
    --multi-release 17 \
    --class-path="BOOT-INF/lib/*" \
    --module-path="BOOT-INF/lib/*" \
    app.jar > /deps
RUN echo $(cat /deps),jdk.crypto.ec > /deps
RUN mkdir /app && cp -r META-INF /app && cp -r BOOT-INF/classes/* /app

FROM openjdk:17-slim-bullseye
COPY --from=0 /deps /deps
RUN jlink \
    --verbose \
    --add-modules $(cat /deps) \
    --strip-java-debug-attributes \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /jre

FROM debian:bullseye-slim
COPY --from=1 /jre /jre
RUN ln -s /jre/bin/java /bin/java
WORKDIR /app
COPY --from=0 /build/BOOT-INF/lib /lib
COPY --from=0 /app .
ENTRYPOINT [ "java", "-cp", ".:/lib/*", "tech.sledger.investments.Investments", "--spring.profiles.active=prod" ]
