FROM openjdk:8-alpine

COPY target/uberjar/xzero.jar /xzero/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/xzero/app.jar"]
