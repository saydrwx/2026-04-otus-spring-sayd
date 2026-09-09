FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder
ENV APP_DIR=/opt/app
RUN mkdir -p $APP_DIR
WORKDIR $APP_DIR
COPY ./pom.xml $APP_DIR
RUN mvn dependency:go-offline
COPY ./src $APP_DIR/src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-alpine
ENV APP_DIR=/opt/app
RUN mkdir -p $APP_DIR
WORKDIR $APP_DIR
COPY --from=builder $APP_DIR/target/hw17-*.jar $APP_DIR/
EXPOSE 8080
CMD ["java", "-jar", "/opt/app/hw17-docker-1.0-SNAPSHOT.jar"]