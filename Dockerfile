FROM arm64v8/debian:bullseye-slim
WORKDIR /build
RUN apt-get update -y && apt-get install wget -y
RUN wget -q https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.3.0/graalvm-ce-java17-linux-aarch64-21.3.0.tar.gz
RUN tar -xzf *.tar.gz
RUN graalvm-ce-java17-21.3.0/bin/jlink --no-header-files --no-man-pages --compress=2 --strip-java-debug-attributes --output /jvm --add-modules \
java.base,java.compiler,java.instrument,java.logging,java.management,java.naming,java.prefs,jdk.net,\
java.rmi,java.security.jgss,java.security.sasl,java.transaction.xa,java.xml,jdk.httpserver,\
jdk.management,jdk.unsupported,jdk.naming.dns,jdk.crypto.ec,java.desktop,java.scripting,java.sql
COPY ./build/libs/*.jar app.jar
RUN graalvm-ce-java17-21.3.0/bin/jar -xf app.jar
RUN mkdir /app && cp -r META-INF /app && cp -r BOOT-INF/classes/* /app

FROM arm64v8/debian:bullseye-slim
WORKDIR /app
COPY --from=0 /jvm /jvm
COPY --from=0 /build/BOOT-INF/lib /lib
COPY --from=0 /app .
ENTRYPOINT [ "/jvm/bin/java", "-cp", ".:/lib/*", "tech.sledger.investments.Investments", "--spring.profiles.active=prod" ]
