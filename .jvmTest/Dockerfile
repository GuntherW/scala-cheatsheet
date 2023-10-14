FROM eclipse-temurin:17-jdk-jammy
COPY *.java /src/
RUN mkdir /app \
    && ls /src \
    && javac /src/PrintXmxXms.java -d /app
CMD ["sh", "-c", \
     "java -version \
      && java -XX:MaxRAMPercentage=50 -cp /app PrintXmxXms"]
#-XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=50
#-Xmx1024m