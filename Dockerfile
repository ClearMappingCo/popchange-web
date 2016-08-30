FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/popchange.jar /popchange/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/popchange/app.jar"]
