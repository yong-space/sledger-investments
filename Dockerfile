FROM arm64v8/openjdk:17
RUN jlink --no-header-files --no-man-pages --compress=2 --strip-java-debug-attributes --output /jvm --add-modules \
java.base,java.compiler,java.instrument,java.logging,java.management,java.naming,java.prefs,jdk.net,\
java.rmi,java.security.jgss,java.security.sasl,java.transaction.xa,java.xml,jdk.httpserver,\
jdk.management,jdk.unsupported,jdk.naming.dns,jdk.crypto.ec,java.desktop,java.scripting,java.sql

WORKDIR /build
COPY ./build/libs/*.jar app.jar
RUN jar -xf app.jar
RUN mkdir /app && cp -r META-INF /app && cp -r BOOT-INF/classes/* /app

FROM arm64v8/debian:stretch-slim
WORKDIR /app
COPY --from=0 /jvm /jvm
COPY --from=0 /build/BOOT-INF/lib /lib
COPY --from=0 /app .
ENTRYPOINT [ "/jvm/bin/java", "-cp", ".:/lib/*", "tech.sledger.investments.Investments", "--spring.profiles.active=prod" ]
